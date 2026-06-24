package com.joshsblocks.backrooms.client;

import com.joshsblocks.backrooms.BackroomsMod;
import com.joshsblocks.backrooms.LurkerEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the Pale Lurker. For now it reuses the vanilla humanoid (zombie) mesh
 * with our own pale texture — a placeholder silhouette until it gets its own
 * model. The static rest pose is fine; bespoke animation can come later.
 */
public class LurkerRenderer extends MobRenderer<LurkerEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(BackroomsMod.MOD_ID, "textures/entity/lurker.png");

	public LurkerRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
