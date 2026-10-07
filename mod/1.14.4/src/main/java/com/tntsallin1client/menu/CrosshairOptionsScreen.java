package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options of the custom crosshair: its shape (a preset or drawn by hand), its size, whether that
 * size ignores the GUI scale, and its color. What changes while aiming at a mob has its own screens
 * ({@link CrosshairTargetColorOptionsScreen}, {@link CrosshairTargetShapeOptionsScreen}).
 */
public class CrosshairOptionsScreen extends CrosshairShapeScreen {
	private static final String KEY = "gui.tntsallin1client.crosshair_options.";
	private static final int MIN_PIXEL_SIZE = 1;
	private static final int MAX_PIXEL_SIZE = 6;
	/** Narrower than the other rows, as in the Fabric versions. */
	private static final int PIXEL_SIZE_SLIDER_WIDTH = 100;

	public CrosshairOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.customCrosshairEnabled, value -> config.customCrosshairEnabled = value);
		addShapeOptions(() -> config.crosshairMode, mode -> config.crosshairMode = mode,
				() -> config.crosshairPreset, preset -> config.crosshairPreset = preset,
				() -> config.crosshairCustomGrid);
		addSlider(KEY + "pixel_size", PIXEL_SIZE_SLIDER_WIDTH, MIN_PIXEL_SIZE, MAX_PIXEL_SIZE, () -> config.crosshairPixelSize, value -> config.crosshairPixelSize = value);
		addToggle(KEY + "ignore_gui_scale", () -> config.crosshairIgnoreGuiScale, value -> config.crosshairIgnoreGuiScale = value);
		addColor(() -> config.customCrosshairColor, argb -> config.customCrosshairColor = argb);
		addLink(KEY + "target_color_button", () -> new CrosshairTargetColorOptionsScreen(this));
		addLink(KEY + "target_shape_button", () -> new CrosshairTargetShapeOptionsScreen(this));
		setResettable(ConfigReset.Feature.CROSSHAIR);
	}
}
