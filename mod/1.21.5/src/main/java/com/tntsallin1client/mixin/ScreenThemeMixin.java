package com.tntsallin1client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.menu.OptionsChrome;
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
	@Inject(method = "renderWithTooltip", at = @At("HEAD"))
	private void tntsallin1client$beginThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ThemedUi.begin((Screen) (Object) this);
	}

	@Inject(method = "renderWithTooltip", at = @At("RETURN"))
	private void tntsallin1client$endThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		// The top bar of the mod's options screens in the client design, drawn after the screen itself
		// - see OptionsChrome for why the screen does not draw it.
		OptionsChrome.renderTopBar((Screen) (Object) this, graphics, mouseX, mouseY, a);
		ThemedUi.end();
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$themedBackground(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (ThemedUi.isThemed(self)) {
			// The scrolling options screens clip everything they draw to their list area, the
			// background included - in a world the margins above and below would show the game
			// (own user report). The background is the whole screen's, so it is drawn unclipped.
			graphics.flush();
			ScissorState clip = new ScissorState();
			clip.copyFrom(RenderSystem.SCISSOR_STATE);
			RenderSystem.disableScissor();
			graphics.fill(0, 0, self.width, self.height, ClientTheme.get().background1);
			graphics.flush();
			RenderSystem.SCISSOR_STATE.copyFrom(clip);
			ci.cancel();
		}
	}
}
