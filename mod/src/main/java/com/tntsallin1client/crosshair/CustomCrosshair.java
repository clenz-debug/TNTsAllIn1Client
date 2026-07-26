package com.tntsallin1client.crosshair;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Phase 5i: replaces the vanilla crosshair sprite with a simple, user-colorable
 * "+" shape. Wired in via {@code GuiMixin}, which cancels vanilla's own
 * {@code Gui#renderCrosshair} when this is enabled - a plain HudElement can't do
 * this, since it would draw in addition to, not instead of, the vanilla sprite.
 * Deliberately doesn't reproduce the attack indicator or the spectator
 * entity-targeting exception vanilla's crosshair has - a reasonable
 * simplification for a "leicht" feature like this one.
 */
public final class CustomCrosshair {
	private static final int SIZE = 15;
	private static final int THICKNESS = 1;

	private CustomCrosshair() {
	}

	public static void render(GuiGraphics guiGraphics) {
		int color = ClientConfig.get().customCrosshairColor;
		int centerX = guiGraphics.guiWidth() / 2;
		int centerY = guiGraphics.guiHeight() / 2;
		int half = SIZE / 2;

		guiGraphics.fill(centerX - half, centerY, centerX + half + 1, centerY + THICKNESS, color);
		guiGraphics.fill(centerX, centerY - half, centerX + THICKNESS, centerY + half + 1, color);
	}
}
