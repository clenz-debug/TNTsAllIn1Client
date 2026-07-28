package com.tntsallin1client.menu;

import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

/**
 * Shared hex-color field parsing/rendering, used by {@link ColorPickerPanel}
 * and the color swatch previews in {@link ColorMenuScreen}'s row list.
 */
public final class ColorPickerHelper {
	private ColorPickerHelper() {
	}

	/** 6 hex digits, no "#" - the alpha byte is never shown, only ever forced on parse. */
	public static String toHexRgb(int argbColor) {
		return String.format("%06X", argbColor & 0xFFFFFF);
	}

	/**
	 * Null unless exactly 6 valid hex digits. Always forces full alpha
	 * (0xFF______) - a bare RGB value passed to GuiGraphics is invisible
	 * (alpha 0), the same bug class as Phase 2's client-name-label issue.
	 */
	public static @Nullable Integer parseHexRgbToArgb(String hex) {
		if (hex.length() != 6) {
			return null;
		}
		try {
			return 0xFF000000 | Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static void drawSwatch(GuiGraphics guiGraphics, int x, int y, int size, int argbColor) {
		guiGraphics.fill(x, y, x + size, y + size, 0xFF000000);
		guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, argbColor);
	}
}
