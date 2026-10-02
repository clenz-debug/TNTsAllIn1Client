package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;

/** Options of the FPS counter: on/off and its text color. */
public class FpsCounterOptionsScreen extends FeatureOptionsScreen {
	public FpsCounterOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.fps_counter_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.fps_counter_options.enabled", () -> config.fpsCounterEnabled, value -> config.fpsCounterEnabled = value);
		setColor(() -> config.fpsCounterTextColor, argb -> config.fpsCounterTextColor = argb);
	}
}
