package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;

/**
 * One equipment slot's durability/count display as its own, independently movable HUD element - the
 * elements of {@link ArmorStatusLayoutMode#INDIVIDUAL}, the alternative to
 * {@link ArmorStatusBundledHud}. Until moved they sit in a column down the left edge, so each starts
 * at a spot of its own to drag from.
 */
public final class ArmorStatusSlotHud extends HudElement {
	private static final int DEFAULT_LEFT = 4;
	private static final int DEFAULT_TOP = 60;
	private static final int DEFAULT_ROW_SPACING = 20;

	private final ArmorStatusSlot slot;

	public ArmorStatusSlotHud(ArmorStatusSlot slot) {
		this.slot = slot;
	}

	@Override
	public String labelKey() {
		return this.slot.labelKey();
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.armorStatusEnabled && config.armorStatusLayoutMode == ArmorStatusLayoutMode.INDIVIDUAL
				&& ArmorStatusHud.isSlotEnabled(config, this.slot);
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.armorStatusLayoutFor(this.slot);
	}

	@Override
	protected boolean isHidden(Minecraft client) {
		return client.options.renderDebug;
	}

	@Override
	public int width(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		return ArmorStatusHud.rowWidth(client.font, config, ArmorStatusHud.buildMaxEntry(config, this.slot));
	}

	@Override
	public int height(Minecraft client) {
		return ArmorStatusHud.rowHeight(client.font, ClientConfig.get());
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_LEFT;
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP + this.slot.ordinal() * DEFAULT_ROW_SPACING;
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		if (client.player == null) {
			return;
		}
		ArmorStatusHud.Entry entry = ArmorStatusHud.buildEntry(config, client.player, this.slot);
		if (entry != null) {
			ArmorStatusHud.drawRow(client, config, entry, 0, 0, ArmorStatusHud.reservedTextWidth(client.font, config, entry));
		}
	}
}
