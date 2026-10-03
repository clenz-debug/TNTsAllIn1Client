package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.hud.ItemCounterHud;
import net.minecraft.client.gui.screen.Screen;

/** Options of the item counter: on/off, which item to count (the held one or a fixed id), the item icon, and the text color. */
public class ItemCounterOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.item_counter_options.";
	private static final int MAX_ID_LENGTH = 64;

	public ItemCounterOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.itemCounterEnabled, value -> config.itemCounterEnabled = value);
		addChoice(KEY + "source", KEY + "source.held", KEY + "source.fixed",
				() -> config.itemCounterUseHeldItem, value -> config.itemCounterUseHeldItem = value);
		addTextField(KEY + "item_id", MAX_ID_LENGTH, () -> config.itemCounterItemId, value -> config.itemCounterItemId = value,
				id -> ItemCounterHud.itemForId(id) != null)
				.onlyIf(() -> !config.itemCounterUseHeldItem);
		addToggle(KEY + "show_item_icon", () -> config.itemCounterShowItemIcon, value -> config.itemCounterShowItemIcon = value);
		setColor(() -> config.itemCounterTextColor, argb -> config.itemCounterTextColor = argb);
		setResettable(ConfigReset.Feature.ITEM_COUNTER);
	}
}
