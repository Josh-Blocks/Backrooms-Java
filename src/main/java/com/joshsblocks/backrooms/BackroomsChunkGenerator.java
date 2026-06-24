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
	private static final int LIGHT_SPACING = 7;    // fluorescent panel lattice in the ceiling

	// Maze layout: a lattice of ROOM_SIZE x ROOM_SIZE rooms separated by 1-block
	// walls, with hashed doorways punched through so the space is navigable.
	private static final int ROOM_SIZE = 4;
	private static final int CELL = ROOM_SIZE + 1; // wall pitch (wall occupies local index 0)
	private static final int DIR_WEST = 1;
	private static final int DIR_NORTH = 2;

	public BackroomsChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/**
	 * True where a maze wall column should stand. The world is a grid of rooms; a
	 * column is wall if it lies on a grid line, unless that part of the wall is a
	 * hashed doorway. Every value is a pure function of world coordinates, so the
	 * maze is identical no matter which chunk asks and seamless across borders.
	 */
	private static boolean isWall(int worldX, int worldZ) {
		int lx = Math.floorMod(worldX, CELL);
		int lz = Math.floorMod(worldZ, CELL);

		// Inside a room — never a wall.
		if (lx != 0 && lz != 0) {
			return false;
		}
		// Grid-line intersections are always solid corner posts.
		if (lx == 0 && lz == 0) {
			return true;
		}

		int cellX = Math.floorDiv(worldX, CELL);
		int cellZ = Math.floorDiv(worldZ, CELL);

		if (lx == 0) {
			// Vertical wall = the west edge of the room to the east; doorway runs along lz.
			return !isDoorway(cellX, cellZ, DIR_WEST, lz);
		}
		// Horizontal wall = the north edge of the room to the south; doorway runs along lx.
		return !isDoorway(cellX, cellZ, DIR_NORTH, lx);
	}

	/** Whether the wall segment for this room edge has its (single) doorway at this offset. */
	private static boolean isDoorway(int cellX, int cellZ, int dir, int offset) {
		long h = hash(cellX, cellZ, dir);
		boolean hasDoor = (h & 0xFFL) < 200L;       // ~78% of walls have a doorway
		int doorPos = 1 + (int) ((h >>> 8) % ROOM_SIZE); // 1..ROOM_SIZE
		return hasDoor && offset == doorPos;
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

	private static boolean isLight(int worldX, int worldZ) {
		return Math.floorMod(worldX, LIGHT_SPACING) == 0 && Math.floorMod(worldZ, LIGHT_SPACING) == 0;
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
