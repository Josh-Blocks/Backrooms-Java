package com.joshsblocks.backrooms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where a player came from when they stepped into the Backrooms, and when they
 * entered. Stored as a persistent data attachment on the player so the return
 * trip lands them exactly where they left and the survival timer survives a
 * relog or server restart.
 */
public record EntryPoint(ResourceKey<Level> dimension, double x, double y, double z, long entryGameTime) {
	public static final Codec<EntryPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(EntryPoint::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(EntryPoint::x),
			Codec.DOUBLE.fieldOf("y").forGetter(EntryPoint::y),
			Codec.DOUBLE.fieldOf("z").forGetter(EntryPoint::z),
			Codec.LONG.fieldOf("entry_game_time").forGetter(EntryPoint::entryGameTime)
	).apply(instance, EntryPoint::new));
}
