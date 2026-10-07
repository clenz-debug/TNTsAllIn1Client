package com.tntsallin1client.crosshair;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Replaces the game's crosshair with either one of {@link CrosshairPreset}'s shapes or a grid the
 * player drew, in a color of their choice. Drawn from {@code GuiMixin} in place of the game's own
 * crosshair picture, so it shows in exactly the situations that one would - and the attack
 * indicator the game draws below it stays.
 *
 * <p>While aiming at a mob that a left click would hit, the crosshair can switch to a second color
 * and/or a second shape - each switched on separately. Size and "ignore GUI scale" are shared by
 * both shapes.
 */
public final class CustomCrosshair {
	private CustomCrosshair() {
	}

	public static void render(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		boolean targeting = isTargetingAttackableMob(client);
		Window window = client.window;
		// The HUD is drawn in GUI-scale units; scaling down by the GUI scale makes the size mean screen pixels.
		float scale = config.crosshairIgnoreGuiScale ? (float) (1.0D / window.getGuiScale()) : 1.0F;

		GlStateManager.pushMatrix();
		GlStateManager.translatef(window.getGuiScaledWidth() / 2.0F, window.getGuiScaledHeight() / 2.0F, 0.0F);
		GlStateManager.scalef(scale, scale, 1.0F);
		CrosshairGrid.render(resolveGrid(config, targeting), 0, 0, config.crosshairPixelSize, resolveColor(config, targeting));
		GlStateManager.popMatrix();
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
	private static boolean isTargetingAttackableMob(Minecraft client) {
		Entity entity = client.crosshairPickEntity;
		return entity instanceof LivingEntity && !(entity instanceof Player);
	}
}
