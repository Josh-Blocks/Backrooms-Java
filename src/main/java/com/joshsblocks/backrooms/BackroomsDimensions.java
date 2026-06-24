package com.joshsblocks.backrooms;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Registers the Backrooms dimension's chunk generator and exposes the level key
 * used to teleport in and out. The dimension itself (its type + which generator
 * it uses) is declared in the JSON under {@code data/joshsblocks_backrooms/}.
 */
public final class BackroomsDimensions {
	private BackroomsDimensions() {}

	/** The level key for the Backrooms; matches {@code data/.../dimension/backrooms.json}. */
	public static final ResourceKey<Level> BACKROOMS = ResourceKey.create(
			Registries.DIMENSION, Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "backrooms"));

	public static void register() {
		// The generator's codec must be registered before datapacks (which reference
		// it by this id) are loaded.
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
				Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "backrooms"),
				BackroomsChunkGenerator.CODEC);

		// Headless self-check (dev only): with -Djoshsblocks.backrooms.debugMaze=true,
		// once the server is up we force columns in the Backrooms to generate and log a
		// top-down map. Verifies the whole chain — dimension JSON, generator codec, and
		// block placement — without a player. Off by default so it never spams real logs.
		if (!Boolean.getBoolean("joshsblocks.backrooms.debugMaze") && !"1".equals(System.getenv("BACKROOMS_DEBUG"))) {
			return;
		}
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel level = server.getLevel(BACKROOMS);
			if (level == null) {
				BackroomsMod.LOGGER.error("[probe] Backrooms dimension NOT found — check registration / JSON.");
				return;
			}
			BlockState floor = level.getBlockState(new BlockPos(8, 0, 8));
			BlockState ceiling = level.getBlockState(new BlockPos(7, 5, 7)); // (7,7) hits the light lattice
			BackroomsMod.LOGGER.info("[probe] Backrooms generated -> floor(8,0,8)={}, ceiling(7,5,7)={}",
					floor.getBlock(), ceiling.getBlock());

			// Render head-height (y=2) as a top-down map: '#' = wall, '.' = walkable.
			// Lets us confirm the maze has rooms + doorways without a player.
			BackroomsMod.LOGGER.info("[probe] maze @ y=2 (x: -4..28, z: -4..28), '#'=wall '.'=floor:");
			BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
			for (int z = -4; z <= 28; z++) {
				StringBuilder row = new StringBuilder();
				for (int x = -4; x <= 28; x++) {
					boolean solid = !level.getBlockState(p.set(x, 2, z)).isAir();
					row.append(solid ? '#' : '.');
				}
				BackroomsMod.LOGGER.info("[probe] {}", row);
			}

			// Headless entity check: spawn a Lurker and confirm the type is registered
			// and constructs (attributes, AI). No client needed.
			LurkerEntity test = ModEntities.LURKER.spawn(level, new BlockPos(2, 1, 2),
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			BackroomsMod.LOGGER.info("[probe] test Lurker spawned ok: {}", test != null);
		});
	}
}
