package com.tntsallin1client.inventory;

import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.itemgroup.ItemGroup;

/**
 * The "Sort" button on the player's own inventory screen - the survival one and the creative
 * screen, whose "Inventory" tab is the only place a creative player sees their inventory - plus the
 * key binding ({@link ModKeyBindings#SORT_INVENTORY}, unbound by default). The button sits just
 * outside the panel's right edge, as in the Fabric versions; this version draws the active effects
 * left of the panel, so there is nothing to keep clear of. Called from `HandledScreenMixin`.
 */
public final class QuickSortUi {
	private static final int BUTTON_ID = 9100;
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_HEIGHT = 20;
	private static final int OUTSIDE_MARGIN = 4;
	/** LWJGL 2's `Keyboard.KEY_NONE`. */
	private static final int UNBOUND = 0;

	private QuickSortUi() {
	}

	private static boolean appliesTo(Screen screen) {
		// The class Legacy Yarn calls InventoryScreen is the common base of exactly those two screens.
		return screen instanceof InventoryScreen && ClientConfig.get().quickSortEnabled;
	}

	/**
	 * Called before every frame: makes sure the button is among the screen's buttons (the creative
	 * screen empties that list after its own setup) and at the panel's edge, which moves when an
	 * effect starts or ends while the screen is open.
	 *
	 * @param button     the screen's sort button so far, null if it has none yet
	 * @param panelRight the panel's right edge
	 * @return the screen's sort button from now on, null if it has none
	 */
	public static ButtonWidget place(Screen screen, List<ButtonWidget> buttons, ButtonWidget button, int panelRight, int panelTop) {
		if (!appliesTo(screen)) {
			if (button != null) {
				buttons.remove(button);
			}
			return null;
		}
		if (button == null) {
			button = new ButtonWidget(BUTTON_ID, 0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.translate("gui.tntsallin1client.sort_button"));
		}
		if (!buttons.contains(button)) {
			buttons.add(button);
		}
		button.x = panelRight + OUTSIDE_MARGIN;
		button.y = panelTop;
		return button;
	}

	/** @return whether the key is the sort key on a screen with the sort button, and the inventory was sorted */
	public static boolean keyPressed(MinecraftClient client, Screen screen, int code) {
		if (!appliesTo(screen) || code == UNBOUND || code != ModKeyBindings.SORT_INVENTORY.getCode()) {
			return false;
		}
		trySort(client, screen);
		return true;
	}

	public static void trySort(MinecraftClient client, Screen screen) {
		if (screen instanceof CreativeInventoryScreen && ((CreativeInventoryScreen) screen).getSelectedTab() != ItemGroup.INVENTORY.getIndex()) {
			// Every other creative tab's slots are a list of items to pick from, not the inventory.
			client.inGameHud.setOverlayMessage(I18n.translate("gui.tntsallin1client.sort_button.wrong_tab"), false);
			return;
		}
		if (client.player.inventory.getCursorStack() != null) {
			// The sorter's clicks all start with an empty cursor - an item already on it would be
			// put down into the first slot clicked.
			client.inGameHud.setOverlayMessage(I18n.translate("gui.tntsallin1client.sort_button.carried_item"), false);
			return;
		}
		InventorySorter.sort(client);
	}
}
