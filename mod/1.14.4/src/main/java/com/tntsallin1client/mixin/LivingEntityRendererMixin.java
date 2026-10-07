package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/**
	 * Freecam: the game leaves out the name of whoever the camera is on - with the camera on the
	 * freecam, the player's own name appears. Wanted above the frozen body in the world, but not on
	 * the figure in the inventory screen, which is the same player drawn a second time. That one is
	 * drawn at exactly 0/0/0; in the world the position is relative to the camera, which is never
	 * exactly where the player's feet are.
	 */
	@Inject(method = "renderName(Lnet/minecraft/world/entity/LivingEntity;DDD)V", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$noOwnNameOnInventoryFigure(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
		if (FreecamHandler.isActive() && entity == Minecraft.getInstance().player && x == 0.0 && y == 0.0 && z == 0.0) {
			ci.cancel();
		}
	}
}
