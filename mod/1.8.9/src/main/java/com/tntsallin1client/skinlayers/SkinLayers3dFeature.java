package com.tntsallin1client.skinlayers;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;

/**
 * Draws the skin layer parts {@link SkinLayers3d#prepare} picked for this player. A feature runs
 * right after the renderer has posed and drawn the model, so every body part already sits where the
 * player's animation puts it.
 */
public final class SkinLayers3dFeature implements FeatureRenderer<AbstractClientPlayerEntity> {
	private final PlayerEntityRenderer renderer;

	public SkinLayers3dFeature(PlayerEntityRenderer renderer) {
		this.renderer = renderer;
	}

	@Override
	public void render(AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch,
			float scale) {
		SkinLayers3d.renderLayers(player, this.renderer.getModel(), scale);
	}

	/** The red flash when hurt, like the model itself gets. */
	@Override
	public boolean combineTextures() {
		return true;
	}
}
