package com.joshsblocks.backrooms;

import java.util.EnumSet;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Quiet — a blind hunter that tracks sound. It chases players who move or
 * sprint and forgets those who go still or crouch. When it has no quarry it just
 * shambles, so it's never quite gone.
 */
public class QuietEntity extends Monster {
	private static final double RANGE = 32.0;

	public QuietEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.33)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.FOLLOW_RANGE, RANGE);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7)); // baseline shamble
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.targetSelector.addGoal(1, new HuntBySoundGoal(this));
	}

	private static boolean isNoisy(Player player) {
		if (player.isSprinting()) {
			return true;
		}
		return !player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistance() > 0.04;
	}

	/** Sets the mob's target to a nearby noisy player; drops it once they stay quiet. */
	private static final class HuntBySoundGoal extends Goal {
		private final QuietEntity mob;
		private Player quarry;
		private int quietTicks;

		HuntBySoundGoal(QuietEntity mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			Player nearest = mob.level().getNearestPlayer(mob, RANGE);
			if (nearest != null && nearest.isAlive() && isNoisy(nearest)) {
				this.quarry = nearest;
				return true;
			}
			return false;
		}

		@Override
		public boolean canContinueToUse() {
			if (quarry == null || !quarry.isAlive() || mob.distanceTo(quarry) > RANGE) {
				return false;
			}
			quietTicks = isNoisy(quarry) ? 0 : quietTicks + 1;
			return quietTicks < 40; // ~2s of silence and it loses you
		}

		@Override
		public void start() {
			mob.setTarget(quarry);
		}

		@Override
		public void stop() {
			mob.setTarget(null);
			quarry = null;
			quietTicks = 0;
		}

		@Override
		public void tick() {
			if (quarry != null) {
				mob.setTarget(quarry);
			}
		}
	}
}
