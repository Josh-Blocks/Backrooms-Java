package com.joshsblocks.backrooms;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registers the mod's entities and their default attributes. */
public final class ModEntities {
	private ModEntities() {}

	public static final ResourceKey<EntityType<?>> LURKER_KEY = ResourceKey.create(
			Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "lurker"));

	public static final EntityType<LurkerEntity> LURKER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, LURKER_KEY,
			EntityType.Builder.of(LurkerEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 2.3F)
					.build(LURKER_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(LURKER, LurkerEntity.createAttributes());
	}
}
