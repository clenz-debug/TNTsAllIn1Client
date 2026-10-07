package com.tntsallin1client.skinlayers;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Draws the skin layer parts {@link SkinLayers3d#prepare} picked for this player. A layer runs
 * right after the renderer has posed and drawn the model, so every body part already sits where the
 * player's animation puts it.
 */
public final class SkinLayers3dFeature extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public SkinLayers3dFeature(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
		super(renderer);
	}

	@Override
	public void render(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch,
			float scale) {
		SkinLayers3d.renderLayers(player, this.getParentModel(), scale);
	}

	/** The red flash when hurt, like the model itself gets. */
	@Override
	public boolean colorsOnDamage() {
		return true;
	}
}
