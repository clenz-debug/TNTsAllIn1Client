package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.gui.screens.Screen;

/** Options of the inventory quick sort: on/off and what to sort by - the button reads as plain "Sort by Name" / "Sort by Item Group". */
public class QuickSortOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.quick_sort_options.";

	public QuickSortOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.quickSortEnabled, value -> config.quickSortEnabled = value);
		addValueSwitch(KEY + "mode.category", KEY + "mode.name", () -> config.quickSortGroupByCategory, value -> config.quickSortGroupByCategory = value);
		setResettable(ConfigReset.Feature.QUICK_SORT);
	}
}
