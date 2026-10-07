package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Freecam: keeps the player's own body visible while the camera is elsewhere. The renderer draws
 * the player playing on this client only if the camera is on that very player (the check right at
 * the start of its `render`, bytecode-checked) - so with the camera on the freecam the body would
 * vanish. While freecam is active the player is treated like any other player here.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	@Redirect(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isLocalPlayer()Z"))
	private boolean tntsallin1client$showOwnBodyWhileFreecam(AbstractClientPlayer player) {
		return player.isLocalPlayer() && !FreecamHandler.isActive();
	}
}
