package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ThemedButton;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla text buttons (plain and cycle buttons) drawn like {@link ThemedButton} on themed screens -
 * see {@link ThemedUi}. Icon buttons ({@link ImageButton} in this version) keep their textures; our own
 * themed buttons draw themselves anyway.
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonThemeMixin extends AbstractWidget {
	protected AbstractButtonThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemed(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		Object self = this;
		boolean textButton = (self instanceof Button && !(self instanceof ImageButton)) || self instanceof CycleButton;
		if (!ThemedUi.active() || !textButton) {
			return;
		}
		boolean highlighted = this.active && this.isHoveredOrFocused();
		ThemedButton.drawFrame(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), highlighted, this.alpha);
		ThemedButton.drawCenteredLabel(graphics, this.getMessage(), this.getX(), this.getY(), this.getWidth(), this.getHeight(),
				ThemedButton.textColor(this.active, this.alpha));
		ci.cancel();
	}
}
