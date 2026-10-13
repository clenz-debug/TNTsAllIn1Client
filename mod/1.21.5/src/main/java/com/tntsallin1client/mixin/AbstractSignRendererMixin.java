package com.tntsallin1client.mixin;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.blocks3d.HangingSignChains;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D chains for hanging signs ({@link HangingSignChains}): drawn right after the sign itself.
 * renderSign scales the model (x1, -1, -1) inside its own push/pop, so the same scale is
 * applied here to land in the sign model's coordinates.
 */
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
	@Shadow
	protected abstract float getSignModelRenderScale();

	@Inject(method = "renderSignWithText", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;renderSign(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model;)V",
			shift = At.Shift.AFTER))
	private void tntsallin1client$render3dChains(SignBlockEntity sign, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
			BlockState blockState, SignBlock signBlock, WoodType woodType, Model model, CallbackInfo ci) {
		if ((Object) this instanceof HangingSignRenderer && Blocks3d.active()) {
			float scale = this.getSignModelRenderScale();
			pose.pushPose();
			pose.scale(scale, -scale, -scale);
			HangingSignChains.render(pose, buffer, light, overlay,
					HangingSignRenderer.AttachmentType.byBlockState(blockState) == HangingSignRenderer.AttachmentType.CEILING_MIDDLE);
			pose.popPose();
		}
	}
}
