package com.joshsblocks.backrooms;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Sound events the mod adds. */
public final class ModSounds {
	private ModSounds() {}

	public static final Identifier HUM_ID = Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "ambient.hum");

	/** The constant fluorescent buzz; played as the Backrooms biome's ambient loop. */
	public static final SoundEvent AMBIENT_HUM = SoundEvent.createVariableRangeEvent(HUM_ID);

	public static void init() {
		Registry.register(BuiltInRegistries.SOUND_EVENT, HUM_ID, AMBIENT_HUM);
	}
}
