package com.joshsblocks.backrooms;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registers the mod's creatures and their default attributes. */
public final class ModEntities {
	private ModEntities() {}

	/** Relentless pursuer — once it has you, it commits. */
	public static final EntityType<LurkerEntity> LURKER = register("lurker", LurkerEntity::new, 0.6F, 2.3F);

	/** Blind sound-hunter — it chases what moves and loses what holds still. */
	public static final EntityType<QuietEntity> QUIET = register("quiet", QuietEntity::new, 0.7F, 2.2F);

	/** Dark ambusher — fast in shadow, crawls under working lights. */
	public static final EntityType<GloamEntity> GLOAM = register("gloam", GloamEntity::new, 0.8F, 2.0F);

	/** Gaze stalker — freezes while watched, rushes the moment you look away. */
	public static final EntityType<StillOneEntity> STILL_ONE = register("still_one", StillOneEntity::new, 0.6F, 2.5F);

	private static <T extends Entity> EntityType<T> register(String name, EntityType.EntityFactory<T> factory,
			float width, float height) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(
				Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(LURKER, LurkerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(QUIET, QuietEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GLOAM, GloamEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STILL_ONE, StillOneEntity.createAttributes());
	}
}
