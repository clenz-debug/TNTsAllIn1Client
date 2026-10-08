package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the skin layer parts {@link SkinLayers3d#prepare} picked for this player. A render layer runs
 * right after {@code LivingEntityRenderer} posed the model for this player, so every body part already
 * sits where the player's animation puts it.
 */
public class SkinLayers3dLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final SkinLayerPart[] PARTS = SkinLayerPart.values();

	public SkinLayers3dLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
			float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		int parts = SkinLayers3d.preparedParts();
		if (parts == 0) {
			return;
		}
		PlayerModel<AbstractClientPlayer> model = this.getParentModel();
		ResourceLocation skin = player.getSkin().texture();
		SkinLayerMesh[] meshes = SkinLayerMeshes.get(skin, model);
		if (meshes == null) {
			return;
		}
		int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0f);
		for (SkinLayerPart part : PARTS) {
			if ((parts & part.bit) != 0) {
				SkinLayers3d.renderPart(pose, buffer, skin, model, part, meshes[part.ordinal()], light, overlay);
			}
		}
	}
}
