package com.tntsallin1client.inventory;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Phase 5c: wires the "sort inventory" action into the player's own inventory
 * screen - both the survival {@link InventoryScreen} and the creative-mode
 * screen's "Inventory" tab, since creative players never see the survival one.
 * Button sits just outside the panel's right edge (mirroring how the vanilla
 * recipe book tab sticks out on the left), plus the (unbound by default)
 * keybind from {@link ModKeyBindings}.
 *
 * <p>The actual click sequence always targets {@code player.inventoryMenu}
 * directly, never whatever screen-specific menu is open: in the creative
 * "Inventory" tab, Mojang wires that tab's slots to wrap the exact same
 * underlying container/index as {@code player.inventoryMenu} (see vanilla's
 * {@code CreativeModeInventoryScreen#selectTab}), so operating on
 * {@code player.inventoryMenu} works identically in both screens and avoids
 * depending on that wiring detail directly.</p>
 */
public final class QuickSortUi {
	private static final int SURVIVAL_PANEL_WIDTH = 176;
	private static final int SURVIVAL_PANEL_HEIGHT = 166;
	private static final int CREATIVE_PANEL_WIDTH = 195;
	private static final int CREATIVE_PANEL_HEIGHT = 136;
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_HEIGHT = 20;
	private static final int OUTSIDE_MARGIN = 4;

	private QuickSortUi() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!ClientConfig.get().quickSortEnabled) {
				return;
			}

			int panelWidth;
			int panelHeight;
			if (screen instanceof InventoryScreen) {
				panelWidth = SURVIVAL_PANEL_WIDTH;
				panelHeight = SURVIVAL_PANEL_HEIGHT;
			} else if (screen instanceof CreativeModeInventoryScreen) {
				panelWidth = CREATIVE_PANEL_WIDTH;
				panelHeight = CREATIVE_PANEL_HEIGHT;
			} else {
				return;
			}

			int left = (scaledWidth - panelWidth) / 2;
			int top = (scaledHeight - panelHeight) / 2;

			Screens.getButtons(screen).add(Button.builder(
					Component.translatable("gui.tntsallin1client.sort_button"),
					button -> trySort(client, screen)
				)
				.bounds(left + panelWidth + OUTSIDE_MARGIN, top, BUTTON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.sort_button.tooltip")))
				.build());

			ScreenKeyboardEvents.afterKeyPress(screen).register((scr, keyEvent) -> {
				if (ModKeyBindings.SORT_INVENTORY.matches(keyEvent)) {
					trySort(client, screen);
				}
			});
		});
	}

	private static void trySort(Minecraft client, Screen screen) {
		if (screen instanceof CreativeModeInventoryScreen creativeScreen
				&& creativeScreen.getSelectedItemGroup().getType() != CreativeModeTab.Type.INVENTORY) {
			// Every other creative tab's slots are a virtual item picker, not the
			// real inventory - clicking through those indices wouldn't sort anything
			// sensible, so bail out with a hint instead of silently doing nothing.
			client.player.displayClientMessage(Component.translatable("gui.tntsallin1client.sort_button.wrong_tab"), true);
			return;
		}

		InventorySorter.sort(client.gameMode, client.player.inventoryMenu, client.player);
	}
}
