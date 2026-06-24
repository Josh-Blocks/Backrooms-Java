package com.joshsblocks.backrooms;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Still One — it does not move while you watch it. Look away, even for a
 * moment, and it closes the distance fast; look back and it's frozen again,
 * a little nearer than before. One goal drives it entirely, so nothing else
 * nudges it while it's meant to be holding perfectly still.
 */
public class StillOneEntity extends Monster {
	private static final double RANGE = 40.0;

	public StillOneEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.45)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FOLLOW_RANGE, RANGE);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new GazeStalkGoal(this));
	}

	private static final class GazeStalkGoal extends Goal {
		private final StillOneEntity mob;
		private int attackCooldown;

		GazeStalkGoal(StillOneEntity mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return mob.level().getNearestPlayer(mob, RANGE) != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			Player player = mob.level().getNearestPlayer(mob, RANGE);
			if (player == null || !player.isAlive()) {
				return;
			}
			mob.getLookControl().setLookAt(player);

			if (isWatchedBy(player)) {
				// Hold perfectly still — stop pathing AND kill horizontal drift.
				mob.getNavigation().stop();
				Vec3 v = mob.getDeltaMovement();
				mob.setDeltaMovement(0.0, v.y, 0.0);
				return;
			}

			// Unobserved: rush in, and strike when adjacent.
			mob.getNavigation().moveTo(player, 1.0);
			if (attackCooldown > 0) {
				attackCooldown--;
			}
			if (attackCooldown == 0 && mob.distanceToSqr(player) < 4.0
					&& mob.level() instanceof ServerLevel serverLevel) {
				player.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(mob), 6.0F);
				attackCooldown = 20;
			}
		}

		/** True if {@code player} is looking roughly at the mob, with a clear line to it. */
		private boolean isWatchedBy(Player player) {
			Vec3 toMob = mob.getEyePosition().subtract(player.getEyePosition()).normalize();
			Vec3 look = player.getViewVector(1.0F).normalize();
			return look.dot(toMob) > 0.6 && mob.hasLineOfSight(player);
		}
	}
}
