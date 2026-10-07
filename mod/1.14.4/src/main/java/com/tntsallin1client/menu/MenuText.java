package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;

/**
 * Text helpers shared by the mod's screens. Their titles and labels are drawn through here, so the
 * client design's font and text color have one place to come in once that design is brought over to
 * this version. Not for HUD previews some option screens draw, which have to keep the HUD's real look.
 */
public final class MenuText {
	private MenuText() {
	}

	public static void centered(String text, int centerX, int y, int color) {
		Font font = Minecraft.getInstance().font;
		font.drawShadow(text, centerX - font.width(text) / 2, y, color);
	}

	public static void text(String text, int x, int y, int color) {
		Minecraft.getInstance().font.drawShadow(text, x, y, color);
	}

	/** Word-wrapped by the width the text has in the font it is drawn in. */
	public static List<String> wrap(String text, int maxWidth) {
		List<String> lines = new ArrayList<String>();
		String line = "";
		for (String word : text.split(" ")) {
			String longer = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && width(longer) > maxWidth) {
				lines.add(line);
				line = word;
			} else {
				line = longer;
			}
		}
		if (!line.isEmpty()) {
			lines.add(line);
		}
		return lines;
	}

	public static int width(String text) {
		return Minecraft.getInstance().font.width(text);
	}

	/** The label of an on/off button, e.g. "FPS Counter: ON" - the same wording vanilla's own option buttons use. */
	public static String onOff(String labelKey, boolean on) {
		return I18n.get(labelKey) + ": " + I18n.get(on ? "options.on" : "options.off");
	}
}
