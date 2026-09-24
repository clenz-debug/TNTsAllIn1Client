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
		boolean highlighted = this.active && this.isHoveredOrFocused();
		drawFrame(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), highlighted, this.alpha);
		renderLabel(graphics, textColor(this.active, this.alpha));
	}

	/** The button box - also used for vanilla buttons on themed screens ({@code AbstractButtonThemeMixin}). */
	public static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height, boolean highlighted, float alpha) {
		ClientTheme theme = ClientTheme.get();
		graphics.fill(x, y, x + width, y + height, ClientTheme.withAlpha(highlighted ? theme.accent1 : theme.background2, alpha));
		graphics.renderOutline(x, y, width, height, ClientTheme.withAlpha(highlighted ? theme.accent4 : theme.accent2, alpha));
	}

	public static int textColor(boolean active, float alpha) {
		return ClientTheme.withAlpha(ClientTheme.get().text, active ? alpha : alpha * 0.5f);
	}

	/** A caption centered in a box, cut to fit - vanilla buttons can carry longer labels than ours. */
	public static void drawCenteredLabel(GuiGraphics graphics, Component message, int x, int y, int width, int height, int color) {
		var font = Minecraft.getInstance().font;
		Component label = ClientFont.fit(font, message.getString(), width - 6);
		graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - font.lineHeight) / 2, color, false);
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
