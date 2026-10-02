package com.tntsallin1client.mixin;

import com.tntsallin1client.hud.HudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws our HUD elements after the game's own HUD, so they sit on top of it. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "render(F)V", at = @At("RETURN"))
	private void tnt$renderClientHud(float tickDelta, CallbackInfo ci) {
		HudElements.renderAll(MinecraftClient.getInstance());
	}
}
