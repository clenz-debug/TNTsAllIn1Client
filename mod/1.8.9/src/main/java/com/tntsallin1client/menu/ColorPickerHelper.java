package com.tntsallin1client.menu;

import net.minecraft.client.gui.DrawableHelper;

/** Hex-color text parsing and the color swatch, shared by the mod's color pickers. */
public final class ColorPickerHelper {
	private ColorPickerHelper() {
	}

	/** 6 hex digits, no "#" - the alpha byte is never shown, only ever forced on parse. */
	public static String toHexRgb(int argbColor) {
		return String.format("%06X", argbColor & 0xFFFFFF);
	}

	/** Null unless exactly 6 valid hex digits. Always forces full alpha (0xFF______). */
	public static Integer parseHexRgbToArgb(String hex) {
		if (hex.length() != 6) {
			return null;
		}
		try {
			return 0xFF000000 | Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static void drawSwatch(int x, int y, int size, int argbColor) {
		DrawableHelper.fill(x, y, x + size, y + size, 0xFF000000);
		DrawableHelper.fill(x + 1, y + 1, x + size - 1, y + size - 1, argbColor);
	}
}
