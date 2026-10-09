package com.tntsallin1client.mixin;

import com.tntsallin1client.skinlayers.SkinLayers3dStateAccess;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** See {@link SkinLayers3dStateAccess}. */
@Mixin(PlayerRenderState.class)
public class PlayerRenderStateMixin implements SkinLayers3dStateAccess {
	@Unique
	private int skinLayers3dParts;

	@Override
	public int tntsallin1client$getSkinLayers3dParts() {
		return this.skinLayers3dParts;
	}

	@Override
	public void tntsallin1client$setSkinLayers3dParts(int parts) {
		this.skinLayers3dParts = parts;
	}
}
