package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Themed screens ({@link ThemedUi}): marks the whole screen render as themed for the widget mixins,
 * and replaces the menu background (panorama/blur/dirt) with the plain theme background, like the
 * client design's title screen.
 */
@Mixin(Screen.class)
public class ScreenThemeMixin {
	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"))
	private void tntsallin1client$beginThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ThemedUi.begin((Screen) (Object) this);
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("RETURN"))
	private void tntsallin1client$endThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ThemedUi.end();
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$themedBackground(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (ThemedUi.isThemed(self)) {
			graphics.fill(0, 0, self.width, self.height, ClientTheme.get().background1);
			ci.cancel();
		}
	}
}
