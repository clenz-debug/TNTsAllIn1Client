package com.tntsallin1client.mixin;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CustomCrosshair;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 2 "first mixin": injects at the tail of the vanilla HUD render method to draw a
 * small visible label, proving the Mixin workflow (annotation processor, refmap, injection)
 * works end to end before anything "real" gets built on top of it. Kept around as an actual
 * feature (toggleable in the mod menu) rather than removed once it stopped being just a proof.
 */
@Mixin(Gui.class)
public class GuiMixin {
	private static boolean loggedInjection = false;

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$onRenderTail(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!loggedInjection) {
			TNTsAllIn1ClientMod.LOGGER.info("[{}] Phase 2 mixin fired: injected into Gui#render.", TNTsAllIn1ClientMod.MOD_ID);
			loggedInjection = true;
		}

		if (!ClientConfig.get().clientNameLabelEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.getDebugOverlay().showDebugScreen()) {
			return;
		}
		guiGraphics.drawString(client.font, "TNT's All-In-1 Client (Mixin active)", 4, 4, ClientConfig.get().clientNameLabelColor);
	}

	/**
	 * Phase 5i: cancel vanilla's own crosshair sprite when the custom one is
	 * enabled, so the two don't draw on top of each other.
	 */
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (ClientConfig.get().customCrosshairEnabled) {
			CustomCrosshair.render(guiGraphics);
			ci.cancel();
		}
	}
}
