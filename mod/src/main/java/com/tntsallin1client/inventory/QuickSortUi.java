package com.tntsallin1client.inventory;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * Phase 5c: wires the "sort inventory" action into the vanilla inventory screen -
 * a button just outside the panel's right edge (mirroring how the vanilla recipe
 * book tab sticks out on the left, so it never overlaps any in-panel texture or
 * label), plus the (unbound by default) keybind from {@link ModKeyBindings}.
 * Scoped to the player's own inventory screen only, not chests/other containers,
 * matching the roadmap's 5c scope.
 */
public final class QuickSortUi {
	private static final int PANEL_WIDTH = 176;
	private static final int PANEL_HEIGHT = 166;
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_HEIGHT = 20;
	private static final int OUTSIDE_MARGIN = 4;

	private QuickSortUi() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof InventoryScreen inventoryScreen) || !ClientConfig.get().quickSortEnabled) {
				return;
			}

			int left = (scaledWidth - PANEL_WIDTH) / 2;
			int top = (scaledHeight - PANEL_HEIGHT) / 2;

			Screens.getButtons(screen).add(Button.builder(
					Component.translatable("gui.tntsallin1client.sort_button"),
					button -> InventorySorter.sort(client.gameMode, inventoryScreen.getMenu(), client.player)
				)
				.bounds(left + PANEL_WIDTH + OUTSIDE_MARGIN, top, BUTTON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.sort_button.tooltip")))
				.build());

			ScreenKeyboardEvents.afterKeyPress(screen).register((scr, keyEvent) -> {
				if (ModKeyBindings.SORT_INVENTORY.matches(keyEvent)) {
					InventorySorter.sort(client.gameMode, inventoryScreen.getMenu(), client.player);
				}
			});
		});
	}
}
