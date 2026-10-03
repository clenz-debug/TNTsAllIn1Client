package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;

/**
 * The marks the game draws inside an entity's hitbox: the line at eye height and the line showing
 * where the entity looks. Each has a switch with its color picker directly below, one group after
 * the other on a scrolling page - like in the Fabric versions (own user request). Both are off until
 * switched on - with the custom hitbox color on, the plain box is the default.
 */
public class HitboxIndicatorsOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.hitbox_indicators_options.";

	public HitboxIndicatorsOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "eye_height", () -> config.customHitboxShowEyeHeight, value -> config.customHitboxShowEyeHeight = value);
		addColor(() -> config.customHitboxEyeHeightColor, argb -> config.customHitboxEyeHeightColor = argb);
		addToggle(KEY + "view_direction", () -> config.customHitboxShowViewDirection, value -> config.customHitboxShowViewDirection = value);
		addColor(() -> config.customHitboxViewDirectionColor, argb -> config.customHitboxViewDirectionColor = argb);
	}
}
