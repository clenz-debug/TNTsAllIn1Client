package com.tntsallin1client.design;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.DrawableHelper;

/**
 * The game's tooltip box with its line in the client font - for the tooltips of the client design's
 * own buttons (the logo's "Design wechseln", the title screen's close button; own user report: they
 * were still in the game's font). Box, colors and placement are the game's ({@code
 * Screen#renderTooltip}, bytecode-read), only the line is measured and drawn in {@link ClientFont}.
 */
public final class ClientTooltip extends DrawableHelper {
	private static final ClientTooltip DRAWER = new ClientTooltip();
	private static final int BACKGROUND = 0xF0100010;
	private static final int BORDER_TOP = 0x505000FF;
	private static final int BORDER_BOTTOM = (BORDER_TOP & 0xFEFEFE) >> 1 | BORDER_TOP & 0xFF000000;
	/** The height of a one-line tooltip's text area. */
	private static final int LINE_HEIGHT = 8;
	/** A capital letter of the client font sits half a pixel lower in its line than one of the game's font. */
	private static final float TEXT_LIFT = 0.5F;

	private ClientTooltip() {
	}

	public static void render(String text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		int width = ClientFont.width(text);
		int x = mouseX + 12;
		int y = mouseY - 12;
		if (x + width > screenWidth) {
			x -= 28 + width;
		}
		if (y + LINE_HEIGHT + 6 > screenHeight) {
			y = screenHeight - LINE_HEIGHT - 6;
		}
		GlStateManager.disableDepthTest();
		DRAWER.zOffset = 300.0F;
		DRAWER.drawBox(x, y, width, LINE_HEIGHT);
		ClientFont.draw(text, x, y - TEXT_LIFT, 0xFFFFFFFF);
		DRAWER.zOffset = 0.0F;
		GlStateManager.enableDepthTest();
	}

	private void drawBox(int x, int y, int width, int height) {
		this.fillGradient(x - 3, y - 4, x + width + 3, y - 3, BACKGROUND, BACKGROUND);
		this.fillGradient(x - 3, y + height + 3, x + width + 3, y + height + 4, BACKGROUND, BACKGROUND);
		this.fillGradient(x - 3, y - 3, x + width + 3, y + height + 3, BACKGROUND, BACKGROUND);
		this.fillGradient(x - 4, y - 3, x - 3, y + height + 3, BACKGROUND, BACKGROUND);
		this.fillGradient(x + width + 3, y - 3, x + width + 4, y + height + 3, BACKGROUND, BACKGROUND);
		this.fillGradient(x - 3, y - 2, x - 2, y + height + 2, BORDER_TOP, BORDER_BOTTOM);
		this.fillGradient(x + width + 2, y - 2, x + width + 3, y + height + 2, BORDER_TOP, BORDER_BOTTOM);
		this.fillGradient(x - 3, y - 3, x + width + 3, y - 2, BORDER_TOP, BORDER_TOP);
		this.fillGradient(x - 3, y + height + 2, x + width + 3, y + height + 3, BORDER_BOTTOM, BORDER_BOTTOM);
	}
}
