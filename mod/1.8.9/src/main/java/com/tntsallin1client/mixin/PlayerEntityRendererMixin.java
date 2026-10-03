package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Freecam: keeps the player's own body visible while the camera is elsewhere. The renderer draws
 * the player playing on this client only if the camera is on that very player (the check right at
 * the start of its `render`) - so with the camera on the freecam the body would vanish. While
 * freecam is active the player is treated like any other player here.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Redirect(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isMainPlayer()Z"))
	private boolean tnt$showOwnBodyWhileFreecam(AbstractClientPlayerEntity player) {
		return player.isMainPlayer() && !FreecamHandler.isActive();
	}
}
