package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CustomCrosshair;

import net.minecraft.client.gui.Gui;
import com.tntsallin1client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the custom crosshair in place of the game's own. */
@Mixin(Gui.class)
public class GuiMixin {
	/**
	 * Phase 5i: cancel vanilla's own crosshair sprite when the custom one is
	 * enabled, so the two don't draw on top of each other.
	 */
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderCrosshair(PoseStack poseStack, CallbackInfo ci) {
		GuiGraphics guiGraphics = GuiGraphics.of(poseStack);
		if (ClientConfig.get().customCrosshairEnabled) {
			CustomCrosshair.render(guiGraphics);
			ci.cancel();
		}
	}
}
