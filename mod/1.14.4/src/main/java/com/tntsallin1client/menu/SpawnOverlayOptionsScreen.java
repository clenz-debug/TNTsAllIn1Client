package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screens.Screen;

/** Options of the light level overlay: on/off, its key, and whether the key is held or switches the overlay on and off. */
public class SpawnOverlayOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.spawn_overlay_options.";

	public SpawnOverlayOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.spawnOverlayEnabled, value -> config.spawnOverlayEnabled = value);
		addKeyBinding(KEY + "key", ModKeyBindings.SPAWN_OVERLAY);
		addChoice(KEY + "mode", KEY + "mode.hold", KEY + "mode.toggle", () -> config.spawnOverlayHoldMode, value -> config.spawnOverlayHoldMode = value);
		setResettable(ConfigReset.Feature.SPAWN_OVERLAY);
	}
}
