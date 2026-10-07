package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options of the keystrokes overlay: on/off, the color a box lights up in, the color of the key
 * labels, and which keys to show. The idle box color is not exposed. Each color has its heading and
 * its picker right on this screen, as in the Fabric versions - the screen scrolls.
 */
public class KeystrokesOptionsScreen extends FeatureOptionsScreen {
	public KeystrokesOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.keystrokes_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.keystrokes_options.enabled", () -> config.keystrokesEnabled, value -> config.keystrokesEnabled = value);
		addHeading("gui.tntsallin1client.keystrokes_options.active_color_label");
		addColor(() -> config.keystrokesActiveColor, argb -> config.keystrokesActiveColor = argb);
		addHeading("gui.tntsallin1client.keystrokes_options.text_color_label");
		addColor(() -> config.keystrokesTextColor, argb -> config.keystrokesTextColor = argb);
		addLink("gui.tntsallin1client.keystrokes_options.keys_button", () -> new KeystrokesKeysOptionsScreen(this));
		setResettable(ConfigReset.Feature.KEYSTROKES);
	}
}
