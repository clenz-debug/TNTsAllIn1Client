package com.tntsallin1client.mixin;

import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.blocks3d.HangingSignChains;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides a hanging sign's own flat chains while {@link HangingSignChains} draws 3D ones instead. The
 * sign models are shared per wood type, so this is set on every use - always the same answer within
 * a frame, whichever sign happens to be drawn first.
 */
@Mixin(HangingSignRenderer.class)
public abstract class HangingSignRendererMixin {
	@Inject(method = "getSignModel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/model/Model$Simple;",
			at = @At("RETURN"))
	private void tntsallin1client$hideFlatChains(BlockState state, WoodType woodType, CallbackInfoReturnable<Model.Simple> cir) {
		HangingSignChains.showVanillaChains(cir.getReturnValue(),
				HangingSignRenderer.AttachmentType.byBlockState(state) == HangingSignRenderer.AttachmentType.CEILING_MIDDLE, !Blocks3d.active());
	}
}
