package com.joshsblocks.backrooms.client;

import com.joshsblocks.backrooms.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-only setup: hooks the Lurker up to its renderer. */
public class BackroomsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.LURKER, LurkerRenderer::new);
	}
}
