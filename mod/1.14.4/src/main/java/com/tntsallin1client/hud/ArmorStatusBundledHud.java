package com.tntsallin1client.hud;

import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;

/**
 * All shown equipment slots as a single HUD element, lined up horizontally or vertically per
 * {@link ClientConfig#armorStatusBundledDirection} - the element of
 * {@link ArmorStatusLayoutMode#BUNDLED}, the alternative to one {@link ArmorStatusSlotHud} per slot.
 *
 * <p>The element's box is as big as the block gets with every shown slot filled. Empty slots are
 * left out, so the block is usually smaller than its box: it sits at the box's top left and grows
 * down/right - or, with {@link ClientConfig#armorStatusBundledReversed}, at the bottom right and
 * grows up/left, for a display docked to the bottom or right screen edge.
 */
public final class ArmorStatusBundledHud extends HudElement {
	private static final int DEFAULT_LEFT = 4;
	private static final int DEFAULT_TOP = 60;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.armor_status";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.armorStatusEnabled && config.armorStatusLayoutMode == ArmorStatusLayoutMode.BUNDLED;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.armorStatusBundledHudLayout;
	}

	@Override
	protected boolean isHidden(Minecraft client) {
		return client.options.renderDebug;
	}

	@Override
	public int width(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		return ArmorStatusHud.bundledWidth(client.font, config, ArmorStatusHud.buildMaxEntries(config));
	}

	@Override
	public int height(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		return ArmorStatusHud.bundledHeight(client.font, config, ArmorStatusHud.buildMaxEntries(config));
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_LEFT;
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		if (client.player == null) {
			return;
		}
		List<ArmorStatusHud.Entry> entries = ArmorStatusHud.buildEntries(config, client.player);
		if (entries.isEmpty()) {
			return;
		}

		int x = 0;
		int y = 0;
		if (config.armorStatusBundledReversed) {
			if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
				y = height(client) - ArmorStatusHud.bundledHeight(client.font, config, entries);
			} else {
				x = width(client) - ArmorStatusHud.bundledWidth(client.font, config, entries);
			}
		}
		ArmorStatusHud.drawBundled(client, config, entries, x, y);
	}
}
