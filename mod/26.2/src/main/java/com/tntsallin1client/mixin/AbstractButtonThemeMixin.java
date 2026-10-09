package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla text buttons (plain and cycle buttons) drawn like {@link ThemedButton} on themed screens -
 * see {@link ThemedUi}. Icon buttons keep their icon and get the themed frame behind it; our own
 * themed buttons draw themselves anyway.
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonThemeMixin extends AbstractWidget {
	protected AbstractButtonThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemed(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		Object self = this;
		boolean textButton = (self instanceof Button && !(self instanceof SpriteIconButton)) || self instanceof CycleButton;
		if (!ThemedUi.active() || !textButton) {
			return;
		}
		boolean highlighted = this.active && this.isHoveredOrFocused();
		ThemedButton.drawFrame(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), highlighted, this.alpha);
		ThemedButton.drawCenteredLabel(graphics, this.getMessage(), this.getX(), this.getY(), this.getWidth(), this.getHeight(),
				ThemedButton.textColor(this.active, this.alpha));
		ci.cancel();
	}

	/**
	 * Icon buttons (since 26.2 the pause menu has a row of them: feedback, bug report, friends, player
	 * reporting): their grey button background becomes the themed frame, the icon on top stays as it
	 * is. Every button draws that background through this method; text buttons never get here, they
	 * are drawn whole above.
	 */
	@Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemedBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		ThemedButton.drawFrame(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active && this.isHoveredOrFocused(), this.alpha);
		ci.cancel();
	}
}
