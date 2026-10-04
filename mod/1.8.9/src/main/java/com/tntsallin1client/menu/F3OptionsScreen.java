package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screen.Screen;

/**
 * Options of the F3 features: the Quick Info block on/off, the key that - pressed with F3 held -
 * shows the system info page ({@link com.tntsallin1client.debug.SystemInfoHud}), and that page's
 * text color.
 */
public class F3OptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.f3_options.";

	public F3OptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.f3QuickInfoEnabled, value -> config.f3QuickInfoEnabled = value);
		addKeyBinding(KEY + "system_info_key", ModKeyBindings.SYSTEM_INFO);
		setColor(() -> config.systemInfoTextColor, argb -> config.systemInfoTextColor = argb);
		setResettable(ConfigReset.Feature.F3_QUICK_INFO);
	}
}
