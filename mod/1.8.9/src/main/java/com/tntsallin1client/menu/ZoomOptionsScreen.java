package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screen.Screen;

/** Options of the zoom: on/off, its key, and how much the mouse is slowed down while zooming. */
public class ZoomOptionsScreen extends FeatureOptionsScreen {
	private static final int MIN_SENSITIVITY_PERCENT = 5;
	private static final int MAX_SENSITIVITY_PERCENT = 100;

	public ZoomOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.zoom_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.zoom_options.enabled", () -> config.zoomEnabled, value -> config.zoomEnabled = value);
		addKeyBinding("gui.tntsallin1client.zoom_options.key", ModKeyBindings.ZOOM);
		addSlider("gui.tntsallin1client.zoom_options.sensitivity", MIN_SENSITIVITY_PERCENT, MAX_SENSITIVITY_PERCENT,
				() -> config.zoomSensitivityPercent, value -> config.zoomSensitivityPercent = value);
		setHint("gui.tntsallin1client.zoom_options.scroll_hint");
	}
}
