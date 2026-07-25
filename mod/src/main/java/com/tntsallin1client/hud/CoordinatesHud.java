package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

/**
 * Phase 5a: always-visible coordinates + facing direction, positioned below the
 * Phase 2 proof-of-mixin label so the two don't overlap. Coordinates, the N/S/W/O
 * direction letter, and the degree number are each independently toggleable via
 * {@link ClientConfig} (see the coordinates HUD options screen).
 */
public class CoordinatesHud implements HudElement {
	private static final String[] COMPASS_DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int LEFT = 4;
	private static final int TOP = 14;

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

		int y = TOP;
		if (config.coordinatesHudShowCoordinates) {
			BlockPos pos = player.blockPosition();
			String coordsLine = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			guiGraphics.drawString(client.font, coordsLine, LEFT, y, TEXT_COLOR);
			y += client.font.lineHeight;
		}

		String facingLine = facingLine(config, player.getYRot());
		if (facingLine != null) {
			guiGraphics.drawString(client.font, facingLine, LEFT, y, TEXT_COLOR);
		}
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
