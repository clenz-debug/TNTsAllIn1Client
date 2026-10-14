package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.blocks3d.HangingSignChains;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D blocks: real chain links on hanging signs instead of the flat crossed sheets - drawn right after
 * the sign's own model, in the same place and scale ({@link HangingSignChains}). The hanging sign
 * renderer inherits {@code renderSignWithText} from this class, hence the mixin here; its flat chains
 * are switched off in {@link HangingSignModelMixin}.
 */
@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {
	@Shadow
	public abstract float getSignModelRenderScale();

	@Inject(method = "renderSignWithText", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/blockentity/SignRenderer;renderSign(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model;)V"))
	private void tntsallin1client$render3dChains(SignBlockEntity sign, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
			BlockState state, SignBlock signBlock, WoodType woodType, Model model, CallbackInfo ci) {
		if ((Object) this instanceof HangingSignRenderer && Blocks3d.active()) {
			float scale = this.getSignModelRenderScale();
			pose.pushPose();
			pose.scale(scale, -scale, -scale);
			HangingSignChains.render(pose, buffer, light, overlay, HangingSignChains.isMiddle(state));
			pose.popPose();
		}
	}
}
