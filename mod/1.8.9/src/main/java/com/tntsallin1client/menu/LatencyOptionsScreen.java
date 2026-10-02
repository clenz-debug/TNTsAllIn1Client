package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;

/** Options of the latency display: on/off and its text color. */
public class LatencyOptionsScreen extends FeatureOptionsScreen {
	public LatencyOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.latency_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.latency_options.enabled", () -> config.latencyHudEnabled, value -> config.latencyHudEnabled = value);
		setColor(() -> config.latencyTextColor, argb -> config.latencyTextColor = argb);
	}
}
