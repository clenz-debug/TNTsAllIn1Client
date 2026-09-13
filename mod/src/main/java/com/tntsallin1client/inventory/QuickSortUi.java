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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Collection;

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
 *
 * <p><b>Effect icon overlap (follow-up feedback):</b> vanilla's own active
 * potion-effect icons ({@code EffectsInInventory}) render in that exact same
 * spot - starting at {@code leftPos + imageWidth + 2}, top-aligned with the
 * panel - so the sort button used to sit right on top of them whenever the
 * player had an effect active. {@link #effectsRowHeight} mirrors that class's
 * own row-height math (33px per row, or {@code 132 / (count - 1)} once there
 * are more than 5 stacked close together) to push the button below however
 * many effect rows are currently showing, instead of guessing a fixed offset.</p>
 */
public final class QuickSortUi {
	private static final int SURVIVAL_PANEL_WIDTH = 176;
	private static final int SURVIVAL_PANEL_HEIGHT = 166;
	private static final int CREATIVE_PANEL_WIDTH = 195;
	private static final int CREATIVE_PANEL_HEIGHT = 136;
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_HEIGHT = 20;
	private static final int OUTSIDE_MARGIN = 4;
	// Matches EffectsInInventory's own "is there enough room to draw effect
	// icons at all" check, so the two agree on whether effects show up here.
	private static final int MIN_EFFECTS_ROOM = 32;

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
			int buttonY = top;

			Collection<MobEffectInstance> activeEffects = client.player.getActiveEffects();
			if (!activeEffects.isEmpty() && scaledWidth - (left + panelWidth + 2) >= MIN_EFFECTS_ROOM) {
				buttonY = top + effectsRowHeight(activeEffects.size()) * activeEffects.size() + OUTSIDE_MARGIN;
			}

			Screens.getButtons(screen).add(Button.builder(
					Component.translatable("gui.tntsallin1client.sort_button"),
					button -> trySort(client, screen)
				)
				.bounds(left + panelWidth + OUTSIDE_MARGIN, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.sort_button.tooltip")))
				.build());

			ScreenKeyboardEvents.afterKeyPress(screen).register((scr, keyEvent) -> {
				if (ModKeyBindings.SORT_INVENTORY.matches(keyEvent)) {
					trySort(client, screen);
				}
			});
		});
	}

	/** Mirrors {@code EffectsInInventory}'s own per-row vertical spacing. */
	private static int effectsRowHeight(int effectCount) {
		return effectCount > 5 ? 132 / (effectCount - 1) : 33;
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

		if (!client.player.inventoryMenu.getCarried().isEmpty()) {
			// InventorySorter assumes an empty cursor at every step (each of its PICKUP
			// clicks either grabs onto or places from an empty cursor) - an item already
			// riding the cursor (e.g. just grabbed from the creative palette) throws that
			// off and silently merges/swaps it into whatever slot gets clicked first,
			// which is exactly the "one item turned into two" duplication bug reported
			// after sorting in creative.
			client.player.displayClientMessage(Component.translatable("gui.tntsallin1client.sort_button.carried_item"), true);
			return;
		}

		InventorySorter.sort(client.gameMode, client.player.inventoryMenu, client.player);
	}
}
