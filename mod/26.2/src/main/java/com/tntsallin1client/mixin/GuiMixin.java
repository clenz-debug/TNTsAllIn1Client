package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CustomCrosshair;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the custom crosshair in place of the game's own. */
@Mixin(Hud.class)
public class GuiMixin {
	/**
	 * Phase 5i: cancel vanilla's own crosshair sprite when the custom one is
	 * enabled, so the two don't draw on top of each other.
	 */
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (ClientConfig.get().customCrosshairEnabled) {
			CustomCrosshair.render(guiGraphics);
			ci.cancel();
		}
	}
}
