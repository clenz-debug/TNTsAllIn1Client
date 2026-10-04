package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;

/**
 * Text helpers shared by the mod's screens. Their titles and labels are drawn through here: in the
 * client font and theme text color while the client design is on ({@link ThemedUi}), the game's own
 * otherwise. For the screens' own text only, not for HUD previews some option screens draw, which
 * have to keep the HUD's real look.
 */
public final class MenuText {
	private MenuText() {
	}

	public static void centered(String text, int centerX, int y, int color) {
		if (ThemedUi.active()) {
			ClientFont.drawCentered(text, centerX, y, ThemedUi.textColor(color));
		} else {
			MinecraftClient client = MinecraftClient.getInstance();
			client.textRenderer.drawWithShadow(text, centerX - client.textRenderer.getStringWidth(text) / 2, y, color);
		}
	}

	public static void text(String text, int x, int y, int color) {
		if (ThemedUi.active()) {
			ClientFont.draw(text, x, y, ThemedUi.textColor(color));
		} else {
			MinecraftClient.getInstance().textRenderer.drawWithShadow(text, x, y, color);
		}
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
		return ThemedUi.active() ? ClientFont.width(text) : MinecraftClient.getInstance().textRenderer.getStringWidth(text);
	}

	/** The label of an on/off button, e.g. "FPS Counter: ON" - the same wording vanilla's own option buttons use. */
	public static String onOff(String labelKey, boolean on) {
		return I18n.translate(labelKey) + ": " + I18n.translate(on ? "options.on" : "options.off");
	}
}
