package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5a: always-visible coordinates + facing direction, positioned below the
 * Phase 2 proof-of-mixin label by default so the two don't overlap. Coordinates,
 * the N/S/W/O direction letter, and the degree number are each independently
 * toggleable via {@link ClientConfig} (see the coordinates HUD options screen).
 * Position/scale can be overridden via {@link HudLayout} (see the HUD editor).
 */
public class CoordinatesHud implements HudElement {
	private static final String[] COMPASS_DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	public static final int DEFAULT_LEFT = 4;
	public static final int DEFAULT_TOP = 14;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.coordinatesHudEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		List<String> lines = buildLines(config, player);
		if (lines.isEmpty()) {
			return;
		}

		HudLayout layout = config.coordinatesHudLayout;
		float x = layout.customPosition ? layout.x : DEFAULT_LEFT;
		float y = layout.customPosition ? layout.y : DEFAULT_TOP;
		drawLines(guiGraphics, client.font, lines, x, y, layout.scale, config.coordinatesHudTextColor);
	}

	/** Lines this HUD would currently show, in render order. Shared with the HUD editor for accurate drag bounds. */
	public static List<String> buildLines(ClientConfig config, LocalPlayer player) {
		List<String> lines = new ArrayList<>(2);
		if (config.coordinatesHudShowCoordinates) {
			BlockPos pos = player.blockPosition();
			lines.add(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
		}

		String facingLine = facingLine(config, player.getYRot());
		if (facingLine != null) {
			lines.add(facingLine);
		}

		return lines;
	}

	public static void drawLines(GuiGraphics guiGraphics, Font font, List<String> lines, float x, float y, float scale, int color) {
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(scale);

		int lineY = 0;
		for (String line : lines) {
			guiGraphics.drawString(font, line, 0, lineY, color);
			lineY += font.lineHeight;
		}

		guiGraphics.pose().popMatrix();
	}

	private static String facingLine(ClientConfig config, float yaw) {
		if (!config.coordinatesHudShowDirection && !config.coordinatesHudShowDegrees) {
			return null;
		}

		float normalized = yaw % 360f;
		if (normalized < 0f) {
			normalized += 360f;
		}

		String direction = config.coordinatesHudShowDirection ? compassLetters(normalized) : null;
		String degrees = config.coordinatesHudShowDegrees ? Math.round(normalized) + "°" : null;

		if (direction != null && degrees != null) {
			return direction + " (" + degrees + ")";
		}
		return direction != null ? direction : degrees;
	}

	private static String compassLetters(float normalizedYaw) {
		int index = (int) Math.floor((normalizedYaw + 22.5f) / 45f) % 8;
		return COMPASS_DIRECTIONS[index];
	}
}
