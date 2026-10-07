package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screens.Screen;

/** Options of the freecam: on/off, its key, the flying speed, and a mouse sensitivity of its own. */
public class FreecamOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.freecam_options.";
	private static final int MIN_SPEED = 1;
	private static final int MAX_SPEED = 50;
	private static final int MIN_SENSITIVITY_PERCENT = 10;
	private static final int MAX_SENSITIVITY_PERCENT = 300;

	public FreecamOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.freecamEnabled, value -> config.freecamEnabled = value);
		addKeyBinding(KEY + "key", ModKeyBindings.TOGGLE_FREECAM);
		addSlider(KEY + "speed", MIN_SPEED, MAX_SPEED, () -> config.freecamSpeed, value -> config.freecamSpeed = value);
		addSlider(KEY + "sensitivity", MIN_SENSITIVITY_PERCENT, MAX_SENSITIVITY_PERCENT,
				() -> config.freecamSensitivityPercent, value -> config.freecamSensitivityPercent = value);
		setResettable(ConfigReset.Feature.FREECAM);
	}
}
