package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;

/**
 * Phase 5j: the light-level-at-your-feet line from 5d's F3 Quick Info block
 * ({@code QuickInfoDebugEntry}), as an always-visible overlay instead of
 * something only visible with F3 open. Deliberately just the single number at
 * the player's own position, not a full in-world per-block tile overlay
 * (which the original idea could also have meant) - that's a much bigger
 * feature and a reasonable simplification for a "leicht-moderat" one.
 * Defaults to the bottom-left corner, away from the other HUD elements.
 */
public class LightLevelHud implements HudElement {
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DEFAULT_LEFT = 4;
	private static final int DEFAULT_BOTTOM_MARGIN = 4;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.lightLevelHudEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null || client.level == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		String label = buildLabel(client, camera);

		HudLayout layout = config.lightLevelHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = DEFAULT_LEFT;
			y = defaultY(guiGraphics.guiHeight(), client.font);
		}
		drawLabel(guiGraphics, client.font, label, x, y, layout.scale);
	}

	/** The label this HUD would currently show - shared with the HUD editor for accurate drag bounds. */
	public static String buildLabel(Minecraft client, Entity camera) {
		BlockPos pos = camera.blockPosition();
		int blockLight = client.level.getBrightness(LightLayer.BLOCK, pos);
		String spawnHint = blockLight < 8 ? " (mobs can spawn)" : "";
		return "Light: " + blockLight + spawnHint;
	}

	/** Default un-customized Y, hugging the bottom of the screen - shared with the HUD editor. */
	public static int defaultY(int guiHeight, Font font) {
		return guiHeight - DEFAULT_BOTTOM_MARGIN - font.lineHeight;
	}

	public static void drawLabel(GuiGraphics guiGraphics, Font font, String label, float x, float y, float scale) {
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(scale);
		guiGraphics.drawString(font, label, 0, 0, TEXT_COLOR);
		guiGraphics.pose().popMatrix();
	}
}
