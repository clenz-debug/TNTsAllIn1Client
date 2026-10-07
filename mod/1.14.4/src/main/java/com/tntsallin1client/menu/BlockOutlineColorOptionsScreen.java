package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screens.Screen;

/** Options of the block outline color: on/off and the color of the outline around the block being looked at. */
public class BlockOutlineColorOptionsScreen extends FeatureOptionsScreen {
	public BlockOutlineColorOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.block_outline_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.block_outline_options.enabled",
				() -> config.customBlockOutlineColorEnabled, value -> config.customBlockOutlineColorEnabled = value);
		setColor(() -> config.customBlockOutlineColor, argb -> config.customBlockOutlineColor = argb);
		setResettable(ConfigReset.Feature.BLOCK_OUTLINE_COLOR);
	}
}
