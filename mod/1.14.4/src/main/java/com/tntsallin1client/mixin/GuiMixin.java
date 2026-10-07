package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CustomCrosshair;
import com.tntsallin1client.hud.HudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws our HUD elements after the game's own HUD, so they sit on top of it, and our crosshair in place of the game's. */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "render", at = @At("RETURN"))
	private void tntsallin1client$renderClientHud(float partialTick, CallbackInfo ci) {
		HudElements.renderAll(Minecraft.getInstance());
	}

	/**
	 * The first picture `renderCrosshair` draws is the crosshair itself (bytecode-checked: the debug
	 * crosshair of F3 is a branch of its own, the attack indicator's pictures come after). Ours is
	 * drawn instead; the game has set an inverting blend mode for its crosshair and the attack
	 * indicator that follows, which drawing ours changes - put back afterwards.
	 */
	@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;blit(IIIIII)V", ordinal = 0))
	private void tntsallin1client$replaceCrosshair(Gui gui, int x, int y, int u, int v, int width, int height) {
		if (!ClientConfig.get().customCrosshairEnabled) {
			gui.blit(x, y, u, v, width, height);
			return;
		}
		CustomCrosshair.render(Minecraft.getInstance());
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
