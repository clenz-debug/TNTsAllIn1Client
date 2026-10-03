package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CustomCrosshair;
import com.tntsallin1client.hud.HudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws our HUD elements after the game's own HUD, so they sit on top of it, and our crosshair in place of the game's. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Shadow
	protected abstract boolean showCrosshair();

	@Inject(method = "render(F)V", at = @At("RETURN"))
	private void tnt$renderClientHud(float tickDelta, CallbackInfo ci) {
		HudElements.renderAll(MinecraftClient.getInstance());
	}

	/**
	 * The game asks here whether to draw its crosshair. Where it would, and ours is switched on, ours
	 * is drawn right away and the game is told not to draw its own.
	 */
	@Redirect(method = "render(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;showCrosshair()Z"))
	private boolean tnt$replaceCrosshair(InGameHud hud) {
		if (!this.showCrosshair()) {
			return false;
		}
		if (!ClientConfig.get().customCrosshairEnabled) {
			return true;
		}
		CustomCrosshair.render(MinecraftClient.getInstance());
		return false;
	}
}
