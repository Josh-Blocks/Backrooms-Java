package com.joshsblocks.backrooms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;

/**
 * Sanity/"stability" pressure — the lore's idea that the Backrooms wear you down
 * the longer you're exposed. You hold steady in the light, but the dark patches
 * (and anything lurking nearby) erode you. As stability falls you get nauseous,
 * then the dark closes in, and at zero the place starts doing real harm. Standing
 * under a working light lets you gather yourself again.
 *
 * <p>Per-player and in-memory: stability resets to full each time you enter, so
 * it's a within-run survival gauge rather than a permanent stat.
 */
public final class BackroomsStability {
	private BackroomsStability() {}

	private static final float MAX = 100.0F;
	private static final int INTERVAL = 10;           // ticks between updates (~2/sec)
	private static final int LIGHT_SAFE = 8;          // block-light at/above this counts as "lit"
	private static final float REGEN_LIT = 1.0F;      // per update under light
	private static final float DRAIN_DARK = 2.5F;     // per update in the dark
	private static final float DRAIN_PER_THREAT = 1.5F;

	private static final Map<UUID, Float> STABILITY = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(BackroomsStability::tick);
	}

	private static void tick(MinecraftServer server) {
		ServerLevel backrooms = server.getLevel(BackroomsDimensions.BACKROOMS);
		if (backrooms == null) {
			return;
		}
		long now = backrooms.getGameTime();
		if (now % INTERVAL != 0L) {
			return;
		}

		Set<UUID> inside = new HashSet<>();
		for (ServerPlayer player : List.copyOf(backrooms.players())) {
			inside.add(player.getUUID());
			float stability = STABILITY.getOrDefault(player.getUUID(), MAX);

			int light = backrooms.getMaxLocalRawBrightness(player.blockPosition());
			stability += (light >= LIGHT_SAFE) ? REGEN_LIT : -DRAIN_DARK;

			int threats = Math.min(3, backrooms.getEntitiesOfClass(Monster.class,
					player.getBoundingBox().inflate(8.0)).size());
			stability -= threats * DRAIN_PER_THREAT;

			stability = Mth.clamp(stability, 0.0F, MAX);
			STABILITY.put(player.getUUID(), stability);

			applyEffects(backrooms, player, stability);
			showMeter(player, stability);
		}

		// Forget anyone who left — stability resets to full on re-entry.
		STABILITY.keySet().removeIf(id -> !inside.contains(id));
	}

	private static void applyEffects(ServerLevel level, ServerPlayer player, float stability) {
		if (stability <= 0.0F) {
			player.hurtServer(level, level.damageSources().magic(), 1.0F);
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
		} else if (stability < 25.0F) {
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0));
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
		} else if (stability < 50.0F) {
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
		}
	}

	private static void showMeter(ServerPlayer player, float stability) {
		int filled = Math.round(stability / 10.0F);
		StringBuilder bar = new StringBuilder("[");
		for (int i = 0; i < 10; i++) {
			bar.append(i < filled ? '=' : '-');
		}
		bar.append(']');
		ChatFormatting colour = stability < 25.0F ? ChatFormatting.RED
				: (stability < 50.0F ? ChatFormatting.GOLD : ChatFormatting.GRAY);
		player.displayClientMessage(
				Component.literal("Stability " + bar + " " + Math.round(stability) + "%").withStyle(colour), true);
	}
}
