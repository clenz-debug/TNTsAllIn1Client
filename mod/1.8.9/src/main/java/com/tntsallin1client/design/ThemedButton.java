package com.tntsallin1client.design;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * Flat, theme-colored button for the client design, as in the Fabric versions - background2 fill
 * with an accent outline, the outline brightening to the lightest accent on hover. What a click
 * does is the screen's business, by the button's id, as with every button in this version.
 */
public class ThemedButton extends ButtonWidget {
	/** 0..1 - for fading in. */
	public float alpha = 1.0F;
	/** Shown at the cursor while it is over the button, if set. */
	public String tooltip;

	public ThemedButton(int id, int x, int y, int width, int height, String message) {
		super(id, x, y, width, height, message);
	}

	@Override
	public void render(MinecraftClient client, int mouseX, int mouseY) {
		if (!this.visible) {
			return;
		}
		this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
		drawFrame(this.x, this.y, this.width, this.height, this.active && this.hovered, this.alpha);
		this.mouseDragged(client, mouseX, mouseY);
		renderLabel(textColor(this.active, this.alpha));
	}

	public int getHeight() {
		return this.height;
	}

	/** The button box. */
	public static void drawFrame(int x, int y, int width, int height, boolean highlighted, float alpha) {
		ClientTheme theme = ClientTheme.get();
		DrawableHelper.fill(x, y, x + width, y + height, ClientTheme.withAlpha(highlighted ? theme.accent1 : theme.background2, alpha));
		int outline = ClientTheme.withAlpha(highlighted ? theme.accent4 : theme.accent2, alpha);
		DrawableHelper.fill(x, y, x + width, y + 1, outline);
		DrawableHelper.fill(x, y + height - 1, x + width, y + height, outline);
		DrawableHelper.fill(x, y + 1, x + 1, y + height - 1, outline);
		DrawableHelper.fill(x + width - 1, y + 1, x + width, y + height - 1, outline);
	}

	public static int textColor(boolean active, float alpha) {
		return ClientTheme.withAlpha(ClientTheme.get().text, active ? alpha : alpha * 0.5F);
	}

	/** The centered caption, cut to fit - overridden by icon-only buttons. */
	protected void renderLabel(int textColor) {
		String label = ClientFont.fit(this.message, this.width - 6);
		ClientFont.drawCentered(label, this.x + this.width / 2.0F, this.y + (this.height - ClientFont.HEIGHT) / 2.0F, textColor);
	}
}
