package com.joshsblocks.backrooms;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Every block the mod adds, plus the matching {@link BlockItem} for each.
 *
 * <p>Note on names: Minecraft 1.21.11 ships non-obfuscated, so this mod is
 * written against <em>official Mojang mappings</em> (e.g. {@code BlockBehaviour
 * .Properties}, {@code Identifier}, {@code setId(...)}) rather than Yarn — Yarn
 * cannot be used on this version. Since 1.21.2 every block/item must have its
 * registry id stamped onto its properties via {@code setId(...)} before
 * construction, which is what {@link #register} does.
 */
public final class ModBlocks {
	private ModBlocks() {}

	/** Mono-yellow wallpaper — the walls of the maze. */
	public static final Block WALLPAPER = register("wallpaper", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.WOOD).strength(0.8F));

	/** Damp office carpet — the floor underfoot. */
	public static final Block DAMP_CARPET = register("damp_carpet", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).sound(SoundType.WOOL).strength(0.6F));

	/** Stained ceiling tile — the low roof overhead. */
	public static final Block CEILING_TILE = register("ceiling_tile", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.STONE).strength(0.8F));

	/** Buzzing fluorescent panel — full brightness, set into the ceiling. */
	public static final Block FLUORESCENT_LIGHT = register("fluorescent_light", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.STONE).strength(0.5F)
					.lightLevel(state -> 15));

	/** The trigger block — right-click it to cross into (or out of) the Backrooms. */
	public static final Block TRIGGER_BLOCK = register("trigger_block", TriggerBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.STONE).strength(1.2F));

	/**
	 * Registers a block (built by {@code factory}) and its block-item under the mod
	 * namespace, stamping the registry id onto both (required since 1.21.2).
	 */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		Identifier id = Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, name);

		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = factory.apply(properties.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return block;
	}

	/** Called from the mod initializer purely to force this class to load and run its static registrations. */
	public static void init() {
		BackroomsMod.LOGGER.info("[Josh's Blocks: Backrooms] Registered {} blocks.", 5);
	}
}
