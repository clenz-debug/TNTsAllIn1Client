package com.tntsallin1client.mixin;

import com.tntsallin1client.TNTsAllIn1ClientMod;

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
 * works end to end before anything "real" gets built on top of it.
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

		Minecraft client = Minecraft.getInstance();
		guiGraphics.drawString(client.font, "TNT's All-In-1 Client (Mixin active)", 4, 4, 0xFFFFFFFF);
	}
}
