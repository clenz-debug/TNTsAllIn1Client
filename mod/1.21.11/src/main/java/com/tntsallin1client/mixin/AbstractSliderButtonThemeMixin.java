package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sliders on themed screens ({@link ThemedUi}): a button-style track with a solid accent handle. */
@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonThemeMixin extends AbstractWidget {
	@Unique
	private static final int HANDLE_WIDTH = 8;

	@Shadow
	protected double value;

	protected AbstractSliderButtonThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		ClientTheme theme = ClientTheme.get();
		boolean highlighted = this.active && this.isHoveredOrFocused();
		ThemedButton.drawFrame(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), false, this.alpha);
		int handleX = this.getX() + (int) (this.value * (this.getWidth() - HANDLE_WIDTH));
		graphics.fill(handleX, this.getY(), handleX + HANDLE_WIDTH, this.getY() + this.getHeight(),
				ClientTheme.withAlpha(highlighted ? theme.accent4 : theme.accent3, this.alpha));
		ThemedButton.drawCenteredLabel(graphics, this.getMessage(), this.getX(), this.getY(), this.getWidth(), this.getHeight(),
				ThemedButton.textColor(this.active, this.alpha));
		ci.cancel();
	}
}
