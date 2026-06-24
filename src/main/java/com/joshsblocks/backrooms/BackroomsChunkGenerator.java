package com.joshsblocks.backrooms;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.server.level.WorldGenRegion;

/**
 * Generates the Backrooms: an endless, flat, low-ceilinged space.
 *
 * <p>This first pass is deliberately <em>trivial</em> — a damp-carpet floor and a
 * ceiling of tiles studded with fluorescent panels, with open air between. It
 * exists to prove the custom-dimension wiring (dimension JSON + this generator's
 * codec + teleport) end to end. The maze walls are layered on in the next pass;
 * {@link #isWall} is the single seam where they slot in.
 */
public class BackroomsChunkGenerator extends ChunkGenerator {
	public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
			).apply(instance, BackroomsChunkGenerator::new));

	// World vertical layout. min_y / height here MUST match the dimension_type JSON.
	private static final int MIN_Y = 0;
	private static final int HEIGHT = 16;
	private static final int FLOOR_Y = 0;
	private static final int CEILING_Y = 5;        // walkable interior is y = 1..4

	// Maze layout: a lattice of ROOM_SIZE x ROOM_SIZE rooms on a CELL-block pitch
	// (the wall sits on local index 0). Each room edge is independently a full
	// wall, a 2-wide doorway, or fully open — the "open" ones merge neighbouring
	// rooms into bigger, irregular halls. Lattice intersections stay as solid
	// support pillars (the classic backrooms columns).
	private static final int ROOM_SIZE = 7;
	private static final int CELL = ROOM_SIZE + 1; // pitch = 8
	private static final int DIR_WEST = 1;
	private static final int DIR_NORTH = 2;
	private static final int DIR_LIGHT = 3;

	// Ceiling lights sit on a 4-block grid (offset into rooms); roughly a quarter
	// are "blown" to leave unsettling dark patches.
	private static final int LIGHT_GRID = 4;
	private static final int LIGHT_OFFSET = 2;

	// Wall-edge states.
	private static final int WALL_FULL = 0;
	private static final int WALL_DOOR = 1;
	private static final int WALL_OPEN = 2;

	public BackroomsChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/**
	 * True where a solid wall or pillar column should stand. Lattice intersections
	 * are always pillars; each room edge is independently a full wall, a 2-wide
	 * doorway, or fully open (merging the two rooms). Pure function of world coords,
	 * so the maze is identical from any chunk and seamless across borders.
	 */
	private static boolean isWall(int worldX, int worldZ) {
		int lx = Math.floorMod(worldX, CELL);
		int lz = Math.floorMod(worldZ, CELL);

		if (lx != 0 && lz != 0) {
			return false; // inside a room
		}
		if (lx == 0 && lz == 0) {
			return true;  // support pillar at every lattice intersection
		}

		int cellX = Math.floorDiv(worldX, CELL);
		int cellZ = Math.floorDiv(worldZ, CELL);

		// A vertical line (lx==0) is the west edge of the cell to its east; a
		// horizontal line (lz==0) is the north edge of the cell to its south.
		int dir = (lx == 0) ? DIR_WEST : DIR_NORTH;
		int along = (lx == 0) ? lz : lx; // position along the wall, 1..ROOM_SIZE
		long h = hash(cellX, cellZ, dir);

		int state = wallState(h);
		if (state == WALL_OPEN) {
			return false; // merged with the neighbouring room
		}
		if (state == WALL_FULL) {
			return true;
		}
		int doorPos = 1 + (int) ((h >>> 8) % (ROOM_SIZE - 1)); // 1..ROOM_SIZE-1
		return !(along == doorPos || along == doorPos + 1);    // 2-wide gap
	}

	/** Full wall / doorway / open, weighted so there are plenty of merges and few dead solid walls. */
	private static int wallState(long h) {
		int r = (int) (h & 0xFFL);
		if (r < 95) {
			return WALL_OPEN;  // ~37% — merge rooms into bigger halls
		}
		if (r < 224) {
			return WALL_DOOR;  // ~50% — a doorway
		}
		return WALL_FULL;      // ~13% — solid
	}

	/** Deterministic 64-bit hash (fmix-style) of a room edge. No world seed: the maze is fixed. */
	private static long hash(int cellX, int cellZ, int dir) {
		long h = cellX * 0x9E3779B97F4A7C15L + cellZ * 0xC2B2AE3D27D4EB4FL + dir * 0x165667B19E3779F9L;
		h ^= (h >>> 33);
		h *= 0xFF51AFD7ED558CCDL;
		h ^= (h >>> 33);
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= (h >>> 33);
		return h;
	}

	/** Ceiling-light panels on a 4-block grid, with ~1/4 blown out to leave dark patches. */
	private static boolean isLight(int worldX, int worldZ) {
		if (Math.floorMod(worldX, LIGHT_GRID) != LIGHT_OFFSET
				|| Math.floorMod(worldZ, LIGHT_GRID) != LIGHT_OFFSET) {
			return false;
		}
		return (hash(worldX, worldZ, DIR_LIGHT) & 0x3L) != 0L; // 3 in 4 lit
	}

	/** The block that belongs at this absolute (x, y, z), or air. Shared by fill + column queries. */
	private static BlockState stateAt(int worldX, int y, int worldZ) {
		if (y == FLOOR_Y) {
			return ModBlocks.DAMP_CARPET.defaultBlockState();
		}
		if (y == CEILING_Y) {
			return isLight(worldX, worldZ)
					? ModBlocks.FLUORESCENT_LIGHT.defaultBlockState()
					: ModBlocks.CEILING_TILE.defaultBlockState();
		}
		if (y > FLOOR_Y && y < CEILING_Y && isWall(worldX, worldZ)) {
			return ModBlocks.WALLPAPER.defaultBlockState();
		}
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
			StructureManager structureManager, ChunkAccess chunk) {
		ChunkPos chunkPos = chunk.getPos();
		int minX = chunkPos.getMinBlockX();
		int minZ = chunkPos.getMinBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				int worldX = minX + lx;
				int worldZ = minZ + lz;
				for (int y = MIN_Y; y < MIN_Y + HEIGHT; y++) {
					BlockState state = stateAt(worldX, y, worldZ);
					if (!state.isAir()) {
						chunk.setBlockState(pos.set(worldX, y, worldZ), state);
					}
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
		BlockState[] column = new BlockState[HEIGHT];
		for (int i = 0; i < HEIGHT; i++) {
			column[i] = stateAt(x, MIN_Y + i, z);
		}
		return new NoiseColumn(MIN_Y, column);
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
		return FLOOR_Y + 1; // surface = standing height on the carpet
	}

	@Override
	public int getGenDepth() {
		return HEIGHT;
	}

	@Override
	public int getMinY() {
		return MIN_Y;
	}

	@Override
	public int getSeaLevel() {
		return MIN_Y;
	}

	// Nothing to do for a hand-built dimension.
	@Override
	public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
			StructureManager structureManager, ChunkAccess chunk) {
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState,
			ChunkAccess chunk) {
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
	}
}
