package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;

/** The crosshair's second color, shown while aiming at a mob a left click would hit: on/off and the color. */
public class CrosshairTargetColorOptionsScreen extends FeatureOptionsScreen {
	public CrosshairTargetColorOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.crosshair_target_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.crosshair_target_options.enabled",
				() -> config.crosshairTargetColorEnabled, value -> config.crosshairTargetColorEnabled = value);
		setColor(() -> config.crosshairTargetColor, argb -> config.crosshairTargetColor = argb);
	}
}
