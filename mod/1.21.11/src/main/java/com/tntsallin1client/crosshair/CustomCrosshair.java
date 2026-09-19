package com.tntsallin1client.crosshair;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Phase 5i, redesigned: replaces the vanilla crosshair sprite with either one
 * of {@link CrosshairPreset}'s shapes or a user-drawn {@link CrosshairMode#CUSTOM}
 * grid, both represented the same way (a small on/off pixel grid, see
 * {@link CrosshairGrid}) so one render routine covers every shape. Wired in
 * via {@code GuiMixin}, which cancels vanilla's own {@code Gui#renderCrosshair}
 * when this is enabled - a plain HudElement can't do this, since it would
 * draw in addition to, not instead of, the vanilla sprite. Deliberately
 * doesn't reproduce the spectator entity-targeting exception vanilla's
 * crosshair has - a reasonable simplification, unrelated to the target-color
 * feature below.
 *
 * <p><b>Target color:</b> when enabled, swaps to a second, separately
 * configured color while the crosshair is over an attackable mob. Uses
 * {@code Minecraft.crosshairPickEntity} - the exact field vanilla's own
 * attack-indicator/swing-cooldown bar already keys off of - filtered to
 * {@link LivingEntity} that isn't the player. Verified in the decompiled
 * sources that this is already properly reach-limited: it's derived from
 * {@code LocalPlayer#raycastHitResult}, which picks entities via
 * {@code entityInteractionRange()} (and any item-specific
 * {@code DataComponents.ATTACK_RANGE} override, e.g. for reach weapons) -
 * exactly "would a left click hit this," no separate distance check needed
 * here.
 *
 * <p><b>Target shape (Phase 5ac):</b> same idea, independently toggleable -
 * swaps to a second preset-or-custom-grid ({@code crosshairTargetShapeXxx}
 * fields) while aiming at an attackable mob, reusing the exact same
 * {@code isTargetingAttackableMob()} check as the color swap. Pixel size and
 * "ignore GUI Scale" stay shared between both shapes - only the grid itself
 * changes, not how big it's drawn.
 *
 * <p><b>Size independent of GUI Scale:</b> HUD rendering already runs inside
 * an ambient pose-stack scale matching the current GUI Scale (confirmed by
 * every other scaled HUD element in this project, e.g. {@code KeystrokesHud}).
 * "Ignore GUI Scale" counteracts that by scaling down by {@code 1/guiScale}
 * before drawing, so {@code crosshairPixelSize} maps to physical pixels
 * instead of GUI-scale-space units.
 */
public final class CustomCrosshair {
	private CustomCrosshair() {
	}

	public static void render(GuiGraphics guiGraphics) {
		ClientConfig config = ClientConfig.get();
		boolean targeting = isTargetingAttackableMob();
		boolean[][] grid = resolveGrid(config, targeting);
		int color = resolveColor(config, targeting);

		Minecraft client = Minecraft.getInstance();
		int guiScale = client.getWindow().getGuiScale();
		float compensation = config.crosshairIgnoreGuiScale && guiScale > 0 ? 1.0F / guiScale : 1.0F;

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(guiGraphics.guiWidth() / 2.0F, guiGraphics.guiHeight() / 2.0F);
		guiGraphics.pose().scale(compensation);
		CrosshairGrid.render(guiGraphics, grid, 0, 0, config.crosshairPixelSize, color);
		guiGraphics.pose().popMatrix();
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
		if (config.crosshairTargetColorEnabled && targeting) {
			return config.crosshairTargetColor;
		}
		return config.customCrosshairColor;
	}

	private static boolean isTargetingAttackableMob() {
		Entity entity = Minecraft.getInstance().crosshairPickEntity;
		return entity instanceof LivingEntity && !(entity instanceof Player);
	}
}
