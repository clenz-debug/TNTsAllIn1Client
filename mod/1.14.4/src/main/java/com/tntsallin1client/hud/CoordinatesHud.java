package com.tntsallin1client.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Always-visible coordinates and facing direction. Coordinates, the compass letters and the degree
 * number are each independently switchable (see the coordinates HUD options screen). Sits in the
 * top-left corner until moved.
 */
public final class CoordinatesHud extends TextHudElement {
	private static final String[] COMPASS_DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final int DEFAULT_TOP = 4;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.coordinates_hud";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.coordinatesHudEnabled;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.coordinatesHudLayout;
	}

	@Override
	protected List<String> lines(Minecraft client, ClientConfig config) {
		LocalPlayer player = client.player;
		if (player == null) {
			return Collections.emptyList();
		}

		List<String> lines = new ArrayList<String>(2);
		if (config.coordinatesHudShowCoordinates) {
			// The block the player's feet are in - the same numbers F3 shows as "Block".
			lines.add(Mth.floor(player.x) + ", " + Mth.floor(player.y) + ", " + Mth.floor(player.z));
		}
		String facingLine = facingLine(config, player.yRot);
		if (facingLine != null) {
			lines.add(facingLine);
		}
		return lines;
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

	@Override
	protected int color(ClientConfig config) {
		return config.coordinatesHudTextColor;
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return EDGE_MARGIN;
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}
}
