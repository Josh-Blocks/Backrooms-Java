package com.joshsblocks.backrooms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The way in and the way out of the Backrooms, plus the survive-the-timer escape.
 *
 * <p>Entering stores the player's origin as a persistent {@link EntryPoint}. While
 * they are inside, a boss-bar timer counts down; when it runs out they are pulled
 * back to exactly where they entered. Right-clicking a trigger block inside lets
 * them bail out early.
 */
public final class BackroomsEscape {
	private BackroomsEscape() {}

	/** How long you must last before the Backrooms spit you back out. */
	public static final int ESCAPE_SECONDS = 120;
	private static final long ESCAPE_TICKS = ESCAPE_SECONDS * 20L;

	public static final AttachmentType<EntryPoint> ENTRY = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "entry_point"), EntryPoint.CODEC);

	/** One countdown bar per player currently inside. */
	private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(BackroomsEscape::tick);
	}

	/** Send a player into the Backrooms, remembering where they came from. */
	public static void enter(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		ServerLevel backrooms = server.getLevel(BackroomsDimensions.BACKROOMS);
		if (backrooms == null) {
			return;
		}
		player.setAttached(ENTRY, new EntryPoint(player.level().dimension(),
				player.getX(), player.getY(), player.getZ(), player.level().getGameTime()));
		player.teleportTo(backrooms, player.getX(), 1.0, player.getZ(),
				Set.of(), player.getYRot(), player.getXRot(), false);
		player.sendSystemMessage(Component.literal("The lights hum overhead. Last " + ESCAPE_SECONDS + " seconds to get out…"));
		BackroomsMod.LOGGER.info("[escape] {} entered the Backrooms.", player.getName().getString());
	}

	/** Pull a player back out to their stored entry point (or a safe overworld spot if it is gone). */
	public static void escape(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		EntryPoint entry = player.getAttached(ENTRY);
		clearBar(player.getUUID());
		player.removeAttached(ENTRY);

		ServerLevel destination = entry != null ? server.getLevel(entry.dimension()) : null;
		if (destination != null) {
			player.teleportTo(destination, entry.x(), entry.y(), entry.z(),
					Set.of(), player.getYRot(), player.getXRot(), false);
		} else {
			ServerLevel overworld = server.getLevel(Level.OVERWORLD);
			int x = player.getBlockX();
			int z = player.getBlockZ();
			int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			player.teleportTo(overworld, x + 0.5, y, z + 0.5,
					Set.of(), player.getYRot(), player.getXRot(), false);
		}
		player.sendSystemMessage(Component.literal("You stumble back out into daylight."));
		BackroomsMod.LOGGER.info("[escape] {} left the Backrooms.", player.getName().getString());
	}

	private static void tick(MinecraftServer server) {
		ServerLevel backrooms = server.getLevel(BackroomsDimensions.BACKROOMS);
		if (backrooms == null) {
			return;
		}
		long now = backrooms.getGameTime();
		// Throttle to twice a second — escape timing to ~0.5s is plenty.
		if (now % 10L != 0L) {
			return;
		}

		maybeSpawnCreature(backrooms, now);

		// Snapshot the player list: escape() teleports players out of this dimension,
		// which mutates the live players() list mid-iteration — iterating it directly
		// would throw ConcurrentModificationException.
		for (ServerPlayer player : List.copyOf(backrooms.players())) {
			EntryPoint entry = player.getAttached(ENTRY);
			if (entry == null) {
				continue; // got here some other way; not on a timer
			}
			long remaining = ESCAPE_TICKS - (now - entry.entryGameTime());
			if (remaining <= 0) {
				escape(player);
			} else {
				updateBar(player, remaining);
			}
		}

		// Drop bars for anyone who is no longer inside (left early, disconnected, escaped).
		Set<UUID> inside = backrooms.players().stream().map(Entity::getUUID).collect(Collectors.toSet());
		BARS.entrySet().removeIf(e -> {
			if (!inside.contains(e.getKey())) {
				e.getValue().removeAllPlayers();
				return true;
			}
			return false;
		});
	}

	/** Every few seconds, let one of the four creatures (picked at random) creep in near a player. */
	private static void maybeSpawnCreature(ServerLevel level, long now) {
		if (now % 100L != 0L) {
			return; // ~once every 5 seconds
		}
		java.util.List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			return;
		}
		net.minecraft.util.RandomSource rng = level.getRandom();
		ServerPlayer target = players.get(rng.nextInt(players.size()));
		int nearby = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
				target.getBoundingBox().inflate(48.0)).size();
		if (nearby >= 3) {
			return; // don't swarm
		}
		int dx = (8 + rng.nextInt(14)) * (rng.nextBoolean() ? 1 : -1);
		int dz = (8 + rng.nextInt(14)) * (rng.nextBoolean() ? 1 : -1);
		net.minecraft.core.BlockPos spawnPos = new net.minecraft.core.BlockPos(
				target.getBlockX() + dx, 1, target.getBlockZ() + dz);

		net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.monster.Monster> type =
				switch (rng.nextInt(4)) {
					case 0 -> ModEntities.QUIET;
					case 1 -> ModEntities.GLOAM;
					case 2 -> ModEntities.STILL_ONE;
					default -> ModEntities.LURKER;
				};
		net.minecraft.world.entity.monster.Monster spawned = type.spawn(level, spawnPos,
				net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		if (spawned != null) {
			BackroomsMod.LOGGER.info("[creature] {} appeared near {}",
					type.toString(), target.getName().getString());
		}
	}

	private static void updateBar(ServerPlayer player, long remainingTicks) {
		ServerBossEvent bar = BARS.get(player.getUUID());
		if (bar == null) {
			bar = new ServerBossEvent(Component.literal("Escape the Backrooms"),
					BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
			bar.addPlayer(player);
			BARS.put(player.getUUID(), bar);
		}
		bar.setProgress(Mth.clamp(remainingTicks / (float) ESCAPE_TICKS, 0.0F, 1.0F));
		int seconds = (int) Math.ceil(remainingTicks / 20.0);
		bar.setName(Component.literal("Escape the Backrooms — " + seconds + "s"));
	}

	private static void clearBar(UUID playerId) {
		ServerBossEvent bar = BARS.remove(playerId);
		if (bar != null) {
			bar.removeAllPlayers();
		}
	}
}
