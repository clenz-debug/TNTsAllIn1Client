package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;

/**
 * The crosshair's second shape, shown while aiming at a mob a left click would hit: on/off and the
 * shape, chosen the same way as the crosshair's own. Size and "ignore GUI scale" are shared with it.
 */
public class CrosshairTargetShapeOptionsScreen extends CrosshairShapeScreen {
	public CrosshairTargetShapeOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.crosshair_target_shape_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.crosshair_target_shape_options.enabled",
				() -> config.crosshairTargetShapeEnabled, value -> config.crosshairTargetShapeEnabled = value);
		addShapeOptions(() -> config.crosshairTargetShapeMode, mode -> config.crosshairTargetShapeMode = mode,
				() -> config.crosshairTargetShapePreset, preset -> config.crosshairTargetShapePreset = preset,
				() -> config.crosshairTargetShapeCustomGrid);
	}
}
