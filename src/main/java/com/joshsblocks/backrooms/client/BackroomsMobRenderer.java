package com.joshsblocks.backrooms.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Monster;

/**
 * A shared humanoid renderer for the Backrooms creatures — same vanilla zombie
 * mesh, different placeholder texture per creature. Keeps the rendering code to
 * one class until any of them earns a bespoke model.
 */
public class BackroomsMobRenderer extends MobRenderer<Monster, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier texture;

	public BackroomsMobRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.texture = texture;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return texture;
	}
}
