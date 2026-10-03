package com.tntsallin1client.crosshair;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Replaces the game's crosshair with either one of {@link CrosshairPreset}'s shapes or a grid the
 * player drew, in a color of their choice. Drawn from {@code InGameHudMixin} in place of the game's
 * own, so it shows in exactly the situations that one would.
 *
 * <p>While aiming at a mob that a left click would hit, the crosshair can switch to a second color
 * and/or a second shape - each switched on separately. Size and "ignore GUI scale" are shared by
 * both shapes.
 */
public final class CustomCrosshair {
	private CustomCrosshair() {
	}

	public static void render(MinecraftClient client) {
		ClientConfig config = ClientConfig.get();
		boolean targeting = isTargetingAttackableMob(client);
		Window window = new Window(client);
		// The HUD is drawn in GUI-scale units; scaling down by the GUI scale makes the size mean screen pixels.
		float scale = config.crosshairIgnoreGuiScale ? 1.0F / window.getScaleFactor() : 1.0F;

		GlStateManager.pushMatrix();
		GlStateManager.translate(window.getWidth() / 2.0F, window.getHeight() / 2.0F, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		CrosshairGrid.render(resolveGrid(config, targeting), 0, 0, config.crosshairPixelSize, resolveColor(config, targeting));
		GlStateManager.popMatrix();

		// Back to what the game's HUD expects after its own crosshair: blending on, nothing tinted.
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static boolean[][] resolveGrid(ClientConfig config, boolean targeting) {
		if (config.crosshairTargetShapeEnabled && targeting) {
			return config.crosshairTargetShapeMode == CrosshairMode.CUSTOM
					? config.crosshairTargetShapeCustomGrid
					: config.crosshairTargetShapePreset.grid();
		}
		return config.crosshairMode == CrosshairMode.CUSTOM ? config.crosshairCustomGrid : config.crosshairPreset.grid();
	}

	private static int resolveColor(ClientConfig config, boolean targeting) {
		return config.crosshairTargetColorEnabled && targeting ? config.crosshairTargetColor : config.customCrosshairColor;
	}

	/** The game itself keeps track of the entity under the crosshair within reach - what a left click would hit. */
	private static boolean isTargetingAttackableMob(MinecraftClient client) {
		Entity entity = client.targetedEntity;
		return entity instanceof LivingEntity && !(entity instanceof PlayerEntity);
	}
}
