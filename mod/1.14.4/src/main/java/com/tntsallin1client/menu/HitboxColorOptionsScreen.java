package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options of the hitbox color: on/off and the color of the box F3+B draws around entities, then the
 * way to the extra marks inside the box ({@link HitboxIndicatorsOptionsScreen}) - in the order the
 * Fabric versions have them (own user request: this screen should look like theirs).
 */
public class HitboxColorOptionsScreen extends FeatureOptionsScreen {
	public HitboxColorOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.hitbox_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.hitbox_options.enabled", () -> config.customHitboxColorEnabled, value -> config.customHitboxColorEnabled = value);
		addColor(() -> config.customHitboxColor, argb -> config.customHitboxColor = argb);
		addLink("gui.tntsallin1client.hitbox_options.indicators_button", () -> new HitboxIndicatorsOptionsScreen(this));
		setResettable(ConfigReset.Feature.HITBOX_COLOR);
	}
}
