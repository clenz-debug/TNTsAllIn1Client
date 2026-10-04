package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screen.Screen;

/**
 * Options of "No fog": which kinds of fog get removed. Blindness isn't listed on purpose - it is an
 * effect, not scenery. (This version has no powder snow.)
 */
public class NoFogOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.no_fog_options.";

	public NoFogOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.noFogEnabled, value -> config.noFogEnabled = value);
		addToggle(KEY + "distance", () -> config.noFogDistance, value -> config.noFogDistance = value);
		addToggle(KEY + "water", () -> config.noFogWater, value -> config.noFogWater = value);
		addToggle(KEY + "lava", () -> config.noFogLava, value -> config.noFogLava = value);
		setResettable(ConfigReset.Feature.NO_FOG);
	}
}
