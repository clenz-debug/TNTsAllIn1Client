package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screen.Screen;

/** Options of the clock: on/off, 24- or 12-hour format, and its text color. */
public class ClockOptionsScreen extends FeatureOptionsScreen {
	public ClockOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.clock_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.clock_options.enabled", () -> config.clockHudEnabled, value -> config.clockHudEnabled = value);
		addChoice("gui.tntsallin1client.clock_options.format",
				"gui.tntsallin1client.clock_options.format.24_hour", "gui.tntsallin1client.clock_options.format.12_hour",
				() -> config.clockHud24Hour, value -> config.clockHud24Hour = value);
		setColor(() -> config.clockTextColor, argb -> config.clockTextColor = argb);
		setResettable(ConfigReset.Feature.CLOCK);
	}
}
