package com.tntsallin1client.design;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;

/**
 * Flat, theme-colored button for the client design, as in the other versions - background2 fill
 * with an accent outline, the outline brightening to the lightest accent on hover. The game's own
 * buttons get the same look on themed screens (`AbstractWidgetThemeMixin`); this class is for
 * buttons of our own that fade in or carry a tooltip.
 */
public class ThemedButton extends Button {
	/** 0..1 - for fading in. */
	public float fade = 1.0F;
	/** Shown at the cursor while it is over the button, if set. */
	public String tooltip;

	public ThemedButton(int x, int y, int width, int height, String message, Runnable onPress) {
		super(x, y, width, height, message, pressed -> onPress.run());
	}

	@Override
	public void renderButton(int mouseX, int mouseY, float partialTick) {
		drawFrame(this.x, this.y, this.width, this.height, this.active && this.isHovered(), this.fade);
		renderLabel(textColor(this.active, this.fade));
	}

	/** The button box. */
	public static void drawFrame(int x, int y, int width, int height, boolean highlighted, float alpha) {
		ClientTheme theme = ClientTheme.get();
		GuiComponent.fill(x, y, x + width, y + height, ClientTheme.withAlpha(highlighted ? theme.accent1 : theme.background2, alpha));
		int outline = ClientTheme.withAlpha(highlighted ? theme.accent4 : theme.accent2, alpha);
		GuiComponent.fill(x, y, x + width, y + 1, outline);
		GuiComponent.fill(x, y + height - 1, x + width, y + height, outline);
		GuiComponent.fill(x, y + 1, x + 1, y + height - 1, outline);
		GuiComponent.fill(x + width - 1, y + 1, x + width, y + height - 1, outline);
	}

	public static int textColor(boolean active, float alpha) {
		return ClientTheme.withAlpha(ClientTheme.get().text, active ? alpha : alpha * 0.5F);
	}

	/** A button's caption in the client font, centered and cut to fit. */
	public static void drawLabel(String message, int x, int y, int width, int height, int textColor) {
		String label = ClientFont.fit(message, width - 6);
		ClientFont.drawCentered(label, x + width / 2.0F, y + (height - ClientFont.HEIGHT) / 2.0F, textColor);
	}

	/** The centered caption - overridden by icon-only buttons. */
	protected void renderLabel(int textColor) {
		drawLabel(getMessage(), this.x, this.y, this.width, this.height, textColor);
	}

	/** The game's buttons tell their width, but not this. */
	public int getHeight() {
		return this.height;
	}
}
