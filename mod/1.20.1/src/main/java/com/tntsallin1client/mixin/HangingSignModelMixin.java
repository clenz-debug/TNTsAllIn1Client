package com.tntsallin1client.mixin;

import com.tntsallin1client.blocks3d.Blocks3d;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D blocks: the hanging sign's own flat chains make way for ours ({@link SignRendererMixin}).
 * {@code evaluateVisibleParts} picks which of the two chain shapes shows, right before every draw.
 */
@Mixin(HangingSignRenderer.HangingSignModel.class)
public abstract class HangingSignModelMixin {
	@Shadow
	@Final
	public ModelPart vChains;

	@Shadow
	@Final
	public ModelPart normalChains;

	@Inject(method = "evaluateVisibleParts", at = @At("TAIL"))
	private void tntsallin1client$hideFlatChains(BlockState state, CallbackInfo ci) {
		if (Blocks3d.active()) {
			this.vChains.visible = false;
			this.normalChains.visible = false;
		}
	}
}
