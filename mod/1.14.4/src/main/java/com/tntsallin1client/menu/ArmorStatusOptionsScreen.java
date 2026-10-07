package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.hud.ArmorStatusColorMode;
import com.tntsallin1client.hud.ArmorStatusDirection;
import com.tntsallin1client.hud.ArmorStatusIconPosition;
import com.tntsallin1client.hud.ArmorStatusLayoutMode;
import com.tntsallin1client.hud.HudLayout;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options of the Armor & Tool Status display: name/icon switches, the number color (fixed, or a
 * durability gradient), and the layout (one movable element per slot, or all bundled into one).
 * Which slots are shown and in which order is set on {@link ArmorStatusSlotsOptionsScreen}.
 */
public class ArmorStatusOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.armor_status_options.";
	private static final int MIN_TEXT_VERTICAL_OFFSET = -8;
	private static final int MAX_TEXT_VERTICAL_OFFSET = 8;

	public ArmorStatusOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.armorStatusEnabled, value -> config.armorStatusEnabled = value);
		addToggle(KEY + "show_name", () -> config.armorStatusShowName, value -> config.armorStatusShowName = value);
		addToggle(KEY + "show_icon", () -> config.armorStatusShowIcon, value -> config.armorStatusShowIcon = value);
		addChoice(KEY + "icon_position", KEY + "icon_position.left", KEY + "icon_position.right",
				() -> config.armorStatusIconPosition == ArmorStatusIconPosition.LEFT,
				left -> config.armorStatusIconPosition = left ? ArmorStatusIconPosition.LEFT : ArmorStatusIconPosition.RIGHT)
				.onlyIf(() -> config.armorStatusShowIcon);
		addSlider(KEY + "text_vertical_offset", MIN_TEXT_VERTICAL_OFFSET, MAX_TEXT_VERTICAL_OFFSET,
				() -> config.armorStatusTextVerticalOffset, value -> config.armorStatusTextVerticalOffset = value)
				.onlyIf(() -> config.armorStatusShowIcon);
		addToggle(KEY + "show_max_durability", () -> config.armorStatusShowMaxDurability, value -> config.armorStatusShowMaxDurability = value);
		addChoice(KEY + "color_mode", KEY + "color_mode.fixed", KEY + "color_mode.gradient",
				() -> config.armorStatusColorMode == ArmorStatusColorMode.FIXED,
				fixed -> config.armorStatusColorMode = fixed ? ArmorStatusColorMode.FIXED : ArmorStatusColorMode.GRADIENT);
		addHeading(KEY + "color_label");
		addColor(() -> config.armorStatusColor, argb -> config.armorStatusColor = argb);
		addChoice(KEY + "layout_mode", KEY + "layout_mode.bundled", KEY + "layout_mode.individual",
				() -> config.armorStatusLayoutMode == ArmorStatusLayoutMode.BUNDLED,
				bundled -> {
					config.armorStatusLayoutMode = bundled ? ArmorStatusLayoutMode.BUNDLED : ArmorStatusLayoutMode.INDIVIDUAL;
					resetPositions(config);
				});
		addChoice(KEY + "direction", KEY + "direction.vertical", KEY + "direction.horizontal",
				() -> config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL,
				vertical -> {
					config.armorStatusBundledDirection = vertical ? ArmorStatusDirection.VERTICAL : ArmorStatusDirection.HORIZONTAL;
					resetPositions(config);
				})
				.onlyIf(() -> config.armorStatusLayoutMode == ArmorStatusLayoutMode.BUNDLED);
		addToggle(KEY + "reversed", () -> config.armorStatusBundledReversed, value -> config.armorStatusBundledReversed = value)
				.onlyIf(() -> config.armorStatusLayoutMode == ArmorStatusLayoutMode.BUNDLED);
		addLink(KEY + "slots_button", () -> new ArmorStatusSlotsOptionsScreen(this));
		setResettable(ConfigReset.Feature.ARMOR_STATUS);
	}

	/**
	 * Back to the start positions (keeping the scale) - the display's shape changes with its layout
	 * and direction, so a spot that fit before could now put it partly off screen, out of reach of
	 * the HUD editor.
	 */
	private static void resetPositions(ClientConfig config) {
		config.armorStatusBundledHudLayout.customPosition = false;
		for (HudLayout layout : config.armorStatusSlotHudLayout.values()) {
			layout.customPosition = false;
		}
	}
}
