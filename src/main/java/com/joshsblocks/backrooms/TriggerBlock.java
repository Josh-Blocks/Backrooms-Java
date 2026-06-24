package com.joshsblocks.backrooms;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-clicking this block crosses you between the overworld and the Backrooms.
 *
 * <p>From outside, it drops you into the maze and starts the survival timer; from
 * inside, it lets you bail out early back to exactly where you entered. The
 * teleport, timer and stored entry point all live in {@link BackroomsEscape}.
 */
public class TriggerBlock extends Block {
	public TriggerBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		if (level.dimension().equals(BackroomsDimensions.BACKROOMS)) {
			BackroomsEscape.escape(serverPlayer);
		} else {
			BackroomsEscape.enter(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}
}
