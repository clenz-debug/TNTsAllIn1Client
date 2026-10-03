package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screen.Screen;

/** Options of the coordinates HUD: on/off, which of its parts to show, and its text color. */
public class CoordinatesHudOptionsScreen extends FeatureOptionsScreen {
	public CoordinatesHudOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.coordinates_hud_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.coordinates_hud_options.enabled", () -> config.coordinatesHudEnabled, value -> config.coordinatesHudEnabled = value);
		addToggle("gui.tntsallin1client.coordinates_hud_options.show_coordinates",
				() -> config.coordinatesHudShowCoordinates, value -> config.coordinatesHudShowCoordinates = value);
		addToggle("gui.tntsallin1client.coordinates_hud_options.show_direction",
				() -> config.coordinatesHudShowDirection, value -> config.coordinatesHudShowDirection = value);
		addToggle("gui.tntsallin1client.coordinates_hud_options.show_degrees",
				() -> config.coordinatesHudShowDegrees, value -> config.coordinatesHudShowDegrees = value);
		setColor(() -> config.coordinatesHudTextColor, argb -> config.coordinatesHudTextColor = argb);
		setResettable(ConfigReset.Feature.COORDINATES_HUD);
	}
}
