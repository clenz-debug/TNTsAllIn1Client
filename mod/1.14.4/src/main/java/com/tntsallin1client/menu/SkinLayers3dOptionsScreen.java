package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import net.minecraft.client.gui.screens.Screen;

/** Options of the 3D skin layers: which parts are drawn in 3D, how far they stand off the body, and up to which distance. */
public class SkinLayers3dOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.skin_layers_3d_options.";

	public SkinLayers3dOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.skinLayers3dEnabled, value -> config.skinLayers3dEnabled = value);
		addToggle(KEY + "head", () -> config.skinLayers3dHead, value -> config.skinLayers3dHead = value);
		addToggle(KEY + "jacket", () -> config.skinLayers3dJacket, value -> config.skinLayers3dJacket = value);
		addToggle(KEY + "sleeves", () -> config.skinLayers3dSleeves, value -> config.skinLayers3dSleeves = value);
		addToggle(KEY + "pants", () -> config.skinLayers3dPants, value -> config.skinLayers3dPants = value);
		addSlider(KEY + "depth", SkinLayers3d.MIN_DEPTH_PERCENT, SkinLayers3d.MAX_DEPTH_PERCENT,
				() -> config.skinLayers3dDepthPercent, value -> config.skinLayers3dDepthPercent = value);
		addSlider(KEY + "distance", SkinLayers3d.MIN_DISTANCE, SkinLayers3d.MAX_DISTANCE,
				() -> config.skinLayers3dDistance, value -> config.skinLayers3dDistance = value);
		setResettable(ConfigReset.Feature.SKIN_LAYERS_3D);
	}
}
