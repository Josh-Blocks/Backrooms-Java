package com.joshsblocks.backrooms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Gloam — it always knows where you are, but it belongs to the dark. In shadow it
 * surges; step under a working light and it can only crawl, so the lit corridors
 * are your refuge and the blown-out patches are where it gets you.
 */
public class GloamEntity extends Monster {
	private static final int LIGHT_SAFE = 8;
	private static final double SPEED_DARK = 0.36;
	private static final double SPEED_LIT = 0.07;

	public GloamEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 22.0)
				.add(Attributes.MOVEMENT_SPEED, SPEED_DARK)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!this.level().isClientSide()) {
			int light = this.level().getMaxLocalRawBrightness(this.blockPosition());
			AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.setBaseValue(light >= LIGHT_SAFE ? SPEED_LIT : SPEED_DARK);
			}
		}
	}
}
