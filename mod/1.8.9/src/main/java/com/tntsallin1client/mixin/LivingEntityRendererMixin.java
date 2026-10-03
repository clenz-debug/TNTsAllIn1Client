package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.ClientUserBadges;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametag logo for sneaking players. The game draws a sneaking player's name itself in this method
 * (faint, hidden behind walls) instead of going through the general one `EntityRendererMixin` hooks -
 * the only place in it where a drawing position is set up and taken down again.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "method_10256(Lnet/minecraft/entity/LivingEntity;DDD)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;popMatrix()V"))
	private void tnt$drawClientBadgeSneaking(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
		if (entity instanceof PlayerEntity) {
			int nameWidth = MinecraftClient.getInstance().textRenderer.getStringWidth(entity.getName().asFormattedString());
			ClientUserBadges.drawBeside((PlayerEntity) entity, nameWidth, 0, true);
		}
	}
}
