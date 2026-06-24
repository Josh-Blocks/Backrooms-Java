package com.joshsblocks.backrooms;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;

/**
 * Dimension rules that make the Backrooms behave like the Backrooms: the place is
 * immutable. You cannot break blocks here, and you cannot place them either — the
 * maze stays whole, so you can't dig shortcuts or wall yourself off from whatever
 * is hunting you. Applies in every game mode.
 */
public final class BackroomsRules {
	private BackroomsRules() {}

	public static void register() {
		// No breaking.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				!level.dimension().equals(BackroomsDimensions.BACKROOMS));

		// No placing: cancel any attempt to use a block item against a block in the
		// Backrooms. Empty-handed interactions (like the trigger block) still work.
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (level.dimension().equals(BackroomsDimensions.BACKROOMS)
					&& player.getItemInHand(hand).getItem() instanceof BlockItem) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}
}
