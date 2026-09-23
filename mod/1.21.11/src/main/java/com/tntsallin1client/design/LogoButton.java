package com.tntsallin1client.design;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * The client logo as a button - the design switch on the title screen (own user request: "ein
 * button als logo getarnt"). Grows slightly on hover so it's recognizably clickable without
 * looking like a regular button.
 */
public class LogoButton extends AbstractButton {
	private static final float HOVER_GROWTH = 0.06f;

	private final Runnable onPress;

	public LogoButton(int x, int y, int height, Runnable onPress) {
		super(x, y, Math.round(ClientLogo.widthFor(height)), height, Component.translatable("gui.tntsallin1client.design.switch"));
		this.onPress = onPress;
		this.setTooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.design.switch")));
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		if (TitleScreenDesign.isRunning()) {
			// The switch animation draws the (moving) logo itself.
			return;
		}
		float growth = this.isHoveredOrFocused() ? HOVER_GROWTH : 0.0f;
		float height = this.getHeight() * (1.0f + growth);
		float x = this.getX() + this.getWidth() / 2.0f - ClientLogo.widthFor(height) / 2.0f;
		float y = this.getY() + this.getHeight() / 2.0f - height / 2.0f;
		ClientLogo.draw(graphics, x, y, height, this.alpha);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
