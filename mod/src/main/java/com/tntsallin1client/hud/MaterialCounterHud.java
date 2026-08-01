package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5b: total count of one item across the player's inventory. Either a
 * fixed item id or whatever is currently in the main hand, per
 * {@link ClientConfig#materialCounterUseHeldItem} (set via the ingame menu).
 * Defaults to hugging the top-right corner (recomputed every frame so it stays
 * put if the label's width changes); {@link HudLayout#customPosition} overrides
 * that with a fixed, draggable position/scale (see the HUD editor).
 */
public class MaterialCounterHud implements HudElement {
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_TOP = 4;
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 2;

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

		String label = buildLabel(config, player);
		if (label == null) {
			return;
		}

		boolean showIcon = config.materialCounterShowItemIcon;
		ItemStack iconStack = showIcon ? new ItemStack(resolveTrackedItem(config, player)) : null;

		HudLayout layout = config.materialCounterHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = defaultX(guiGraphics.guiWidth(), client.font, label, showIcon);
			y = DEFAULT_TOP;
		}
		drawLabel(guiGraphics, client.font, label, iconStack, x, y, layout.scale, config.materialCounterTextColor);
	}

	/** Right-aligned default X for the un-customized position - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX(int guiWidth, Font font, String label, boolean showIcon) {
		return guiWidth - DEFAULT_RIGHT_MARGIN - contentWidth(font, label, showIcon);
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}

	/** Unscaled pixel width of the whole row (icon + gap + text, or just text) - shared with the HUD editor's drag bounds. */
	public static int contentWidth(Font font, String label, boolean showIcon) {
		int textWidth = font.width(label);
		return showIcon ? textWidth + ICON_SIZE + ICON_GAP : textWidth;
	}

	/** Unscaled pixel height of the whole row - the 16px icon is taller than a text line once it's shown. */
	public static int contentHeight(Font font, boolean showIcon) {
		return showIcon ? Math.max(ICON_SIZE, font.lineHeight) : font.lineHeight;
	}

	public static void drawLabel(GuiGraphics guiGraphics, Font font, String label, @Nullable ItemStack iconStack,
			float x, float y, float scale, int color) {
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(scale);
		if (iconStack != null) {
			guiGraphics.renderItem(iconStack, 0, 0);
			int textY = (ICON_SIZE - font.lineHeight) / 2;
			guiGraphics.drawString(font, label, ICON_SIZE + ICON_GAP, textY, color);
		} else {
			guiGraphics.drawString(font, label, 0, 0, color);
		}
		guiGraphics.pose().popMatrix();
	}

	/** The label this HUD would currently show, or null if there's nothing sensible to count. */
	public static String buildLabel(ClientConfig config, LocalPlayer player) {
		Item item = resolveTrackedItem(config, player);
		if (item == null || item == Items.AIR) {
			return null;
		}

		int count = countInInventory(player, item);
		return item.getName().getString() + ": " + count;
	}

	private static Item resolveTrackedItem(ClientConfig config, LocalPlayer player) {
		if (config.materialCounterUseHeldItem) {
			return player.getMainHandItem().getItem();
		}

		Identifier itemId = Identifier.tryParse(config.materialCounterItemId);
		return itemId == null ? null : BuiltInRegistries.ITEM.getValue(itemId);
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
