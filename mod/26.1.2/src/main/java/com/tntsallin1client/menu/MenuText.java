package com.tntsallin1client.menu;

import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Titles and labels of the mod's own screens - in the client font and theme text color while the
 * client design is on ({@link ThemedUi}), plain vanilla otherwise. Used for the screens' own text
 * only, not for HUD previews some option screens draw, which have to keep the HUD's real look.
 */
final class MenuText {
	private MenuText() {
	}

	static void centered(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
		if (ThemedUi.active()) {
			graphics.centeredText(font, ClientFont.of(text), x, y, ThemedUi.textColor(color));
		} else {
			graphics.centeredText(font, text, x, y, color);
		}
	}

	static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
		text(graphics, font, text, x, y, color, true);
	}

	static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
		if (ThemedUi.active()) {
			graphics.text(font, ClientFont.of(text), x, y, ThemedUi.textColor(color), false);
		} else {
			graphics.text(font, text, x, y, color, shadow);
		}
	}
}
