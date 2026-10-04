package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the cape on the player's back while sneaking, the way the newer versions do it (own user
 * request). This version hangs the cape at the neck of the upright body whatever the body does:
 * for a sneaking player it only swings the cape 25 degrees further out and slides it two pixels
 * along itself, so its top edge floats above and behind the back, which has gone down and leans
 * forward. In the newer versions the cape is part of the body and moves with it - here the body's
 * sneaking pose is applied to the cape first, and the two makeshift adjustments are left out.
 */
@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {
	private static final String RENDER = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFFF)V";
	/** How far the model draws its body lower while sneaking, in blocks. */
	private static final float SNEAK_BODY_DROP = 0.2F;
	/** The body's forward lean while sneaking - the model's 0.5 radians. */
	private static final float SNEAK_BODY_LEAN_DEGREES = 28.647890F;
	/** The two pixels the model slides the cape along itself for a sneaking player, in blocks. */
	private static final float SNEAK_CAPE_SLIDE = 0.125F;

	/** Whether the player whose cape is being drawn sneaks - set at the start of each `render`. */
	@Unique
	private boolean tnt$sneaking;

	@Inject(method = RENDER, at = @At("HEAD"))
	private void tnt$noteSneaking(AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float age, float headYaw,
			float headPitch, float scale, CallbackInfo ci) {
		this.tnt$sneaking = player.isSneaking();
	}

	/** The move to the back of the body, where the cape hangs - for a sneaking player from where that body is now. */
	@Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V", ordinal = 0))
	private void tnt$followSneakingBody(float x, float y, float z) {
		if (this.tnt$sneaking) {
			GlStateManager.translate(0.0F, SNEAK_BODY_DROP, 0.0F);
			GlStateManager.rotate(SNEAK_BODY_LEAN_DEGREES, 1.0F, 0.0F, 0.0F);
		}
		GlStateManager.translate(x, y, z);
	}

	/** The game asks this to swing a sneaking player's cape further out - not needed on a cape that leans with the body. */
	@Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isSneaking()Z"))
	private boolean tnt$noExtraSwing(AbstractClientPlayerEntity player) {
		return false;
	}

	@Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/PlayerEntityModel;renderCape(F)V"))
	private void tnt$undoCapeSlide(PlayerEntityModel model, float scale) {
		if (this.tnt$sneaking) {
			GlStateManager.translate(0.0F, -SNEAK_CAPE_SLIDE, 0.0F);
		}
		model.renderCape(scale);
	}
}
