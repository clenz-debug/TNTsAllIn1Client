package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Phase 5b: total count of one configured item across the player's inventory,
 * top-right corner. Which item is tallied comes from {@link ClientConfig} for
 * now; the ingame menu (5e) will replace direct JSON edits with a picker.
 */
public class MaterialCounterHud implements HudElement {
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int RIGHT_MARGIN = 4;
	private static final int TOP = 4;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.materialCounterEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		Identifier itemId = Identifier.tryParse(config.materialCounterItemId);
		if (itemId == null) {
			return;
		}

		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		if (item == Items.AIR) {
			// Unset or unknown item id in the config - nothing sensible to count.
			return;
		}

		int count = countInInventory(player, item);
		String label = item.getName().getString() + ": " + count;

		int x = guiGraphics.guiWidth() - RIGHT_MARGIN - client.font.width(label);
		guiGraphics.drawString(client.font, label, x, TOP, TEXT_COLOR);
	}

	private static int countInInventory(LocalPlayer player, Item item) {
		int total = 0;
		NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
		for (ItemStack stack : items) {
			if (stack.getItem() == item) {
				total += stack.getCount();
			}
		}

		ItemStack offhand = player.getOffhandItem();
		if (offhand.getItem() == item) {
			total += offhand.getCount();
		}

		return total;
	}
}
