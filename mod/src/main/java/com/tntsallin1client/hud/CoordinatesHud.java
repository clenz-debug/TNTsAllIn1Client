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
 * Phase 2 proof-of-mixin label so the two don't overlap.
 */
public class CoordinatesHud implements HudElement {
	private static final String[] COMPASS_DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int LEFT = 4;
	private static final int TOP = 14;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (!ClientConfig.get().coordinatesHudEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		BlockPos pos = player.blockPosition();
		String coordsLine = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
		String facingLine = compassDirection(player.getYRot());

		guiGraphics.drawString(client.font, coordsLine, LEFT, TOP, TEXT_COLOR);
		guiGraphics.drawString(client.font, facingLine, LEFT, TOP + client.font.lineHeight, TEXT_COLOR);
	}

	private static String compassDirection(float yaw) {
		float normalized = yaw % 360f;
		if (normalized < 0f) {
			normalized += 360f;
		}
		int index = (int) Math.floor((normalized + 22.5f) / 45f) % 8;
		return COMPASS_DIRECTIONS[index] + " (" + Math.round(normalized) + "°)";
	}
}
