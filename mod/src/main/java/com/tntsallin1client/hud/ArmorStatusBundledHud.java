package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * All enabled equipment slots bundled into a single HUD element, laid out
 * horizontally or vertically per {@link ClientConfig#armorStatusBundledDirection} -
 * only actually renders while {@link ClientConfig#armorStatusLayoutMode} is
 * {@link ArmorStatusLayoutMode#BUNDLED}, the alternative to six separate
 * {@link ArmorStatusSlotHud} elements.
 */
public class ArmorStatusBundledHud implements HudElement {
	private static final int DEFAULT_LEFT = 4;
	private static final int DEFAULT_TOP = 60;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.armorStatusEnabled || config.armorStatusLayoutMode != ArmorStatusLayoutMode.BUNDLED) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		List<ArmorStatusHud.Entry> entries = ArmorStatusHud.buildEntries(config, player);
		if (entries.isEmpty()) {
			return;
		}

		HudLayout layout = config.armorStatusBundledHudLayout;
		float x = layout.customPosition ? layout.x : defaultX();
		float y = layout.customPosition ? layout.y : defaultY();
		ArmorStatusHud.drawBundled(guiGraphics, client.font, config, entries, x, y, layout.scale);
	}

	public static int defaultX() {
		return DEFAULT_LEFT;
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}
}
