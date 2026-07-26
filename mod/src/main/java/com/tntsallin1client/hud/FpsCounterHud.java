package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Phase 5g: always-visible FPS counter, so it's not necessary to open F3 just
 * to see the frame rate. Defaults to the top-right corner, below where
 * {@link MaterialCounterHud} sits so the two don't overlap by default.
 */
public class FpsCounterHud implements HudElement {
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_TOP = 16;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.fpsCounterEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		String label = buildLabel(client);

		HudLayout layout = config.fpsCounterHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = defaultX(guiGraphics.guiWidth(), client.font, label);
			y = DEFAULT_TOP;
		}
		drawLabel(guiGraphics, client.font, label, x, y, layout.scale);
	}

	/** The label this HUD would currently show - shared with the HUD editor for accurate drag bounds. */
	public static String buildLabel(Minecraft client) {
		return client.getFps() + " FPS";
	}

	/** Right-aligned default X for the un-customized position - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX(int guiWidth, Font font, String label) {
		return guiWidth - DEFAULT_RIGHT_MARGIN - font.width(label);
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}

	public static void drawLabel(GuiGraphics guiGraphics, Font font, String label, float x, float y, float scale) {
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(scale);
		guiGraphics.drawString(font, label, 0, 0, TEXT_COLOR);
		guiGraphics.pose().popMatrix();
	}
}
