package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.blocks3d.HangingSignChains;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D blocks: real chain links on hanging signs instead of the flat crossed sheets - drawn right after
 * the sign's own model, in the same place and scale ({@link HangingSignChains}). Hanging signs are an
 * experimental 1.20 feature in 1.19.4 (data pack "Update 1.20"); their renderer draws the model itself
 * at scale 1 - there is no {@code renderSignWithText}/{@code getSignModelRenderScale} yet, so the hook sits
 * in its own {@code render}. Its flat chains are switched off in {@link HangingSignModelMixin}.
 */
@Mixin(HangingSignRenderer.class)
public abstract class HangingSignRendererMixin {
	@Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/blockentity/HangingSignRenderer;renderSign(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIFLnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model;)V"))
	private void tntsallin1client$render3dChains(SignBlockEntity sign, float partialTick, PoseStack pose, MultiBufferSource buffer,
			int light, int overlay, CallbackInfo ci) {
		if (Blocks3d.active()) {
			pose.pushPose();
			pose.scale(1.0f, -1.0f, -1.0f);
			HangingSignChains.render(pose, buffer, light, overlay, HangingSignChains.isMiddle(sign.getBlockState()));
			pose.popPose();
		}
	}
}
