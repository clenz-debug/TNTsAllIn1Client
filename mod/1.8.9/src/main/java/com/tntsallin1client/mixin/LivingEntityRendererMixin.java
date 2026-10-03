package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.friends.ClientUserBadges;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The method that draws a living entity's name (`method_10256` in this name table). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/**
	 * Freecam: the game leaves out the name of whoever the camera is on - with the camera on the
	 * freecam, the player's own name appears. Wanted above the frozen body in the world, but not on
	 * the figure in the inventory screen, which is the same player drawn a second time. That one is
	 * drawn at exactly 0/0/0; in the world the position is relative to the camera, which is never
	 * exactly where the player's feet are.
	 */
	@Inject(method = "method_10256(Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("HEAD"), cancellable = true)
	private void tnt$noOwnNameOnInventoryFigure(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
		if (FreecamHandler.isActive() && entity == MinecraftClient.getInstance().player && x == 0.0 && y == 0.0 && z == 0.0) {
			ci.cancel();
		}
	}

	/**
	 * Nametag logo for sneaking players (see {@link ClientUserBadges}). The game draws a sneaking
	 * player's name itself in this method (faint, hidden behind walls) instead of going through the
	 * general one `EntityRendererMixin` hooks - the only place in it where a drawing position is set
	 * up and taken down again.
	 */
	@Inject(method = "method_10256(Lnet/minecraft/entity/LivingEntity;DDD)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;popMatrix()V"))
	private void tnt$drawClientBadgeSneaking(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
		if (entity instanceof PlayerEntity) {
			int nameWidth = MinecraftClient.getInstance().textRenderer.getStringWidth(entity.getName().asFormattedString());
			ClientUserBadges.drawBeside((PlayerEntity) entity, nameWidth, 0, true);
		}
	}
}
