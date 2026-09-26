package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.blocks3d.HangingSignChains;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D chains for hanging signs ({@link HangingSignChains}): drawn right after the sign itself. In
 * 1.21.11 submitSign scales the model (x1, -1, -1) inside its own push/pop, so the same scale is
 * applied here to land in the sign model's coordinates.
 */
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
	@Shadow
	protected abstract float getSignModelRenderScale();

	@Inject(method = "submitSignWithText", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSign(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
			shift = At.Shift.AFTER))
	private void tntsallin1client$submit3dChains(SignRenderState state, PoseStack pose, BlockState blockState, SignBlock signBlock, WoodType woodType,
			Model.Simple model, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling, SubmitNodeCollector collector, CallbackInfo ci) {
		if ((Object) this instanceof HangingSignRenderer && Blocks3d.active()) {
			float scale = this.getSignModelRenderScale();
			pose.pushPose();
			pose.scale(scale, -scale, -scale);
			HangingSignChains.submit(pose, collector, state.lightCoords,
					HangingSignRenderer.AttachmentType.byBlockState(blockState) == HangingSignRenderer.AttachmentType.CEILING_MIDDLE, crumbling);
			pose.popPose();
		}
	}
}
