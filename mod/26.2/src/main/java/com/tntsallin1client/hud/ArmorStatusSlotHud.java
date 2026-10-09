package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * One equipment slot's durability/count display as its own, independently
 * draggable HUD element - only actually renders while
 * {@link ClientConfig#armorStatusLayoutMode} is {@link ArmorStatusLayoutMode#INDIVIDUAL}
 * (the alternative to {@link ArmorStatusBundledHud}). Six instances are
 * registered, one per {@link ArmorStatusSlot} (see {@code TNTsAllIn1ClientMod}); each
 * checks the layout mode and its own slot's on/off toggle itself, so nothing
 * outside this class needs to know which mode is currently active. Defaults
 * to a vertical stack down the left edge, staggered by slot order so all six
 * start at a distinct position to drag from instead of stacked on top of
 * each other.
 */
public class ArmorStatusSlotHud implements HudElement {
	private static final int DEFAULT_LEFT = 4;
	private static final int DEFAULT_TOP = 60;
	private static final int DEFAULT_ROW_SPACING = 20;

	private final ArmorStatusSlot slot;

	public ArmorStatusSlotHud(ArmorStatusSlot slot) {
		this.slot = slot;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.armorStatusEnabled || config.armorStatusLayoutMode != ArmorStatusLayoutMode.INDIVIDUAL) {
			return;
		}
		if (!ArmorStatusHud.isSlotEnabled(config, this.slot)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		ArmorStatusHud.Entry entry = ArmorStatusHud.buildEntry(config, player, this.slot);
		if (entry == null) {
			return;
		}

		HudLayout layout = config.armorStatusLayoutFor(this.slot);
		float x = layout.customPosition ? layout.x : defaultX();
		float y = layout.customPosition ? layout.y : defaultY(this.slot);

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(layout.scale);
		ArmorStatusHud.drawRow(guiGraphics, client.font, entry, 0, 0, config.armorStatusShowIcon, config.armorStatusIconPosition, config.armorStatusTextVerticalOffset, ArmorStatusHud.reservedTextWidth(client.font, config, entry));
		guiGraphics.pose().popMatrix();
	}

	/** Un-customized default X, same for every slot - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX() {
		return DEFAULT_LEFT;
	}

	/** Un-customized default Y, staggered by slot order - shared with the HUD editor for accurate drag bounds. */
	public static int defaultY(ArmorStatusSlot slot) {
		return DEFAULT_TOP + slot.ordinal() * DEFAULT_ROW_SPACING;
	}
}
