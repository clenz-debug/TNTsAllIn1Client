package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screens.Screen;

/** Options of the shulker box preview: on/off and the key to hold for it. */
public class ShulkerPreviewOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.shulker_options.";

	public ShulkerPreviewOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.shulkerPreviewEnabled, value -> config.shulkerPreviewEnabled = value);
		addKeyBinding(KEY + "key", ModKeyBindings.SHULKER_PREVIEW);
	}
}
