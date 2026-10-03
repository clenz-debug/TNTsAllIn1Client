package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;

/**
 * Options of the keystrokes overlay: on/off, the color a box lights up in, the color of the key
 * labels, and which keys to show. The idle box color is not exposed. Two colors don't fit one
 * screen here, so each has its own ({@link ColorOptionsScreen}).
 */
public class KeystrokesOptionsScreen extends FeatureOptionsScreen {
	public KeystrokesOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.keystrokes_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.keystrokes_options.enabled", () -> config.keystrokesEnabled, value -> config.keystrokesEnabled = value);
		addLink("gui.tntsallin1client.keystrokes_options.active_color_label",
				() -> new ColorOptionsScreen(this, "gui.tntsallin1client.keystrokes_options.active_color_label",
						() -> config.keystrokesActiveColor, argb -> config.keystrokesActiveColor = argb));
		addLink("gui.tntsallin1client.keystrokes_options.text_color_label",
				() -> new ColorOptionsScreen(this, "gui.tntsallin1client.keystrokes_options.text_color_label",
						() -> config.keystrokesTextColor, argb -> config.keystrokesTextColor = argb));
		addLink("gui.tntsallin1client.keystrokes_options.keys_button", () -> new KeystrokesKeysOptionsScreen(this));
	}
}
