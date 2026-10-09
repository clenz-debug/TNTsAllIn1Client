package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the skin layer parts {@link SkinLayers3d#prepare} picked for this player. A render layer runs
 * right after {@code LivingEntityRenderer} posed the model for this state, so every body part already
 * sits where the player's animation puts it.
 */
public class SkinLayers3dLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final SkinLayerPart[] PARTS = SkinLayerPart.values();

	public SkinLayers3dLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		int parts = ((SkinLayers3dStateAccess) state).tntsallin1client$getSkinLayers3dParts();
		if (parts == 0) {
			return;
		}
		PlayerModel model = this.getParentModel();
		ResourceLocation skin = state.skin.body().texturePath();
		SkinLayerMesh[] meshes = SkinLayerMeshes.get(skin, model);
		if (meshes == null) {
			return;
		}
		// The red flash when hurt, like the model itself gets
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0f);

		pose.pushPose();
		model.root().translateAndRotate(pose);
		for (SkinLayerPart part : PARTS) {
			if ((parts & part.bit) != 0) {
				SkinLayers3d.submitPart(pose, collector, skin, model, part, meshes[part.ordinal()], light, overlay);
			}
		}
		pose.popPose();
	}
}
