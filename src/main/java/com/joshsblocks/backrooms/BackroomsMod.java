package com.joshsblocks.backrooms;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for Josh's Blocks: Backrooms.
 *
 * <p>Fabric calls {@link #onInitialize()} once, on both client and dedicated
 * server, during mod loading. Registration of blocks, items, the dimension and
 * the entity all hangs off this method as the mod grows.
 */
public class BackroomsMod implements ModInitializer {
	/** Namespace for every block, item, asset and data file this mod registers. */
	public static final String MOD_ID = "joshsblocks_backrooms";

	/** One shared logger so log lines are clearly attributed to this mod. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// If you see this line in the log, the whole toolchain (Loom -> Gradle ->
		// Loader -> Mojang mappings -> Fabric API -> JDK 21) is wired up correctly.
		LOGGER.info("[Josh's Blocks: Backrooms] Initialised — the lights are humming.");

		// Order matters: blocks must register before the creative tab (whose icon
		// references a block) and before the dimension generator (which places them).
		ModBlocks.init();
		ModEntities.init();
		BackroomsDimensions.register();
		BackroomsEscape.register();
		ModItemGroups.init();
	}
}
