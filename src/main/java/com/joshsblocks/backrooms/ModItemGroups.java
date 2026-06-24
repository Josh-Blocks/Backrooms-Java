package com.joshsblocks.backrooms;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The mod's single creative-inventory tab, holding all Backrooms blocks. */
public final class ModItemGroups {
	private ModItemGroups() {}

	public static final ResourceKey<CreativeModeTab> GENERAL = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "general"));

	public static void init() {
		CreativeModeTab tab = FabricItemGroup.builder()
				.icon(() -> new ItemStack(ModBlocks.TRIGGER_BLOCK))
				.title(Component.translatable("itemGroup." + BackroomsMod.MOD_ID + ".general"))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GENERAL, tab);

		ItemGroupEvents.modifyEntriesEvent(GENERAL).register(entries -> {
			entries.accept(ModBlocks.WALLPAPER);
			entries.accept(ModBlocks.DAMP_CARPET);
			entries.accept(ModBlocks.CEILING_TILE);
			entries.accept(ModBlocks.FLUORESCENT_LIGHT);
			entries.accept(ModBlocks.TRIGGER_BLOCK);
		});
	}
}
