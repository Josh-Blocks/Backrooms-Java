package com.joshsblocks.backrooms.client;

import com.joshsblocks.backrooms.BackroomsMod;
import com.joshsblocks.backrooms.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.resources.Identifier;

/** Client-only setup: hooks every creature up to its renderer. */
public class BackroomsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.LURKER, LurkerRenderer::new);
		EntityRendererRegistry.register(ModEntities.QUIET, ctx -> new BackroomsMobRenderer(ctx, tex("quiet")));
		EntityRendererRegistry.register(ModEntities.GLOAM, ctx -> new BackroomsMobRenderer(ctx, tex("gloam")));
		EntityRendererRegistry.register(ModEntities.STILL_ONE, ctx -> new BackroomsMobRenderer(ctx, tex("still_one")));
	}

	private static Identifier tex(String name) {
		return Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "textures/entity/" + name + ".png");
	}
}
