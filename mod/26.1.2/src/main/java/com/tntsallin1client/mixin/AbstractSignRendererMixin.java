package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.blocks3d.HangingSignChains;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.HangingSignRenderState;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.HangingSignBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D chains for hanging signs ({@link HangingSignChains}): drawn right after the sign itself, while
 * the pose is still the sign model's own (submitSignWithText pushes the body transformation around
 * submitSign and pops it afterwards).
 */
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
	@Inject(method = "submitSignWithText", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSign(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
			shift = At.Shift.AFTER))
	private void tntsallin1client$submit3dChains(SignRenderState state, PoseStack pose, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling,
			SubmitNodeCollector collector, CallbackInfo ci) {
		if (state instanceof HangingSignRenderState hanging && Blocks3d.active()) {
			HangingSignChains.submit(pose, collector, state.lightCoords, hanging.attachmentType == HangingSignBlock.Attachment.CEILING_MIDDLE, crumbling);
		}
	}
}
