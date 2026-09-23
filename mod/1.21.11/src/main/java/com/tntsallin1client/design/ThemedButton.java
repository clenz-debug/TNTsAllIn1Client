package com.tntsallin1client.design;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * Flat, theme-colored button for the client design (own user sketch: plain outlined boxes) -
 * background2 fill with an accent outline, the outline brightening to the lightest accent on hover.
 * Fades with {@link #setAlpha} like any vanilla widget.
 */
public class ThemedButton extends AbstractButton {
	private final Runnable onPress;

	public ThemedButton(int x, int y, int width, int height, Component message, Runnable onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		ClientTheme theme = ClientTheme.get();
		boolean highlighted = this.active && this.isHoveredOrFocused();
		int fill = ClientTheme.withAlpha(highlighted ? theme.accent1 : theme.background2, this.alpha);
		int border = ClientTheme.withAlpha(highlighted ? theme.accent4 : theme.accent2, this.alpha);
		int text = ClientTheme.withAlpha(theme.text, this.active ? this.alpha : this.alpha * 0.5f);

		graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), fill);
		graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), border);
		renderLabel(graphics, text);
	}

	/** The centered caption - overridden by icon-only buttons. */
	protected void renderLabel(GuiGraphics graphics, int text) {
		var font = Minecraft.getInstance().font;
		Component label = ClientFont.of(this.getMessage());
		graphics.drawString(font, label, this.getX() + (this.getWidth() - font.width(label)) / 2,
				this.getY() + (this.getHeight() - font.lineHeight) / 2, text, false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
