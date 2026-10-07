package com.tntsallin1client.inventory;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The "Sort" button on the player's own inventory screen - the survival one and the creative
 * screen, whose "Inventory" tab is the only place a creative player sees their inventory - plus the
 * key binding ({@link ModKeyBindings#SORT_INVENTORY}, unbound by default). The button sits just
 * outside the panel's right edge, as in the other versions; this version draws the active effects
 * left of the panel, so there is nothing to keep clear of. Called from `AbstractContainerScreenMixin`.
 */
public final class QuickSortUi {
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_HEIGHT = 20;
	private static final int OUTSIDE_MARGIN = 4;

	private QuickSortUi() {
	}

	public static boolean appliesTo(Screen screen) {
		// The common base of exactly those two screens.
		return screen instanceof EffectRenderingInventoryScreen && ClientConfig.get().quickSortEnabled;
	}

	public static Button createButton(Screen screen) {
		return new Button(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.get("gui.tntsallin1client.sort_button"),
				pressed -> trySort(Minecraft.getInstance(), screen));
	}

	/** At the panel's edge, which moves when an effect starts or ends or the recipe book opens while the screen is open. */
	public static void position(Button button, int panelRight, int panelTop) {
		button.x = panelRight + OUTSIDE_MARGIN;
		button.y = panelTop;
	}

	/** @return whether the key is the sort key on a screen with the sort button, and the inventory was sorted */
	public static boolean keyPressed(Minecraft client, Screen screen, int key, int scanCode) {
		if (!appliesTo(screen) || !ModKeyBindings.SORT_INVENTORY.matches(key, scanCode)) {
			return false;
		}
		trySort(client, screen);
		return true;
	}

	public static void trySort(Minecraft client, Screen screen) {
		if (screen instanceof CreativeModeInventoryScreen
				&& ((CreativeModeInventoryScreen) screen).getSelectedTab() != CreativeModeTab.TAB_INVENTORY.getId()) {
			// Every other creative tab's slots are a list of items to pick from, not the inventory.
			client.gui.setOverlayMessage(new TextComponent(I18n.get("gui.tntsallin1client.sort_button.wrong_tab")), false);
			return;
		}
		if (!client.player.inventory.getCarried().isEmpty()) {
			// The sorter's clicks all start with an empty cursor - an item already on it would be
			// put down into the first slot clicked.
			client.gui.setOverlayMessage(new TextComponent(I18n.get("gui.tntsallin1client.sort_button.carried_item")), false);
			return;
		}
		InventorySorter.sort(client);
	}
}
