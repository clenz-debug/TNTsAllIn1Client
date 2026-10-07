package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Total count of one item across the player's inventory (off hand included) - either a fixed item or
 * whatever is held right now, per {@link ClientConfig#itemCounterUseHeldItem}. Sits at the right edge
 * below the other displays there until moved.
 */
public final class ItemCounterHud extends HudElement {
	private static final int EDGE_MARGIN = 4;
	private static final int DEFAULT_TOP = 40;
	private static final int ICON_GAP = 2;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.item_counter";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.itemCounterEnabled;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.itemCounterHudLayout;
	}

	@Override
	protected boolean isHidden(Minecraft client) {
		return client.options.renderDebug;
	}

	@Override
	public int width(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		String label = label(client.player, trackedItem(config, client.player));
		if (label == null) {
			return 0;
		}
		int textWidth = client.font.width(label);
		return config.itemCounterShowItemIcon ? ItemIcons.SIZE + ICON_GAP + textWidth : textWidth;
	}

	@Override
	public int height(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		if (trackedItem(config, client.player) == null) {
			return 0;
		}
		return config.itemCounterShowItemIcon ? Math.max(ItemIcons.SIZE, client.font.lineHeight) : client.font.lineHeight;
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return screenWidth - EDGE_MARGIN - width(client);
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		Item tracked = trackedItem(config, client.player);
		String label = label(client.player, tracked);
		if (label == null) {
			return;
		}
		if (config.itemCounterShowItemIcon) {
			ItemIcons.draw(client, new ItemStack(tracked), 0, 0);
			client.font.drawShadow(label, ItemIcons.SIZE + ICON_GAP, (ItemIcons.SIZE - client.font.lineHeight) / 2, config.itemCounterTextColor);
		} else {
			client.font.drawShadow(label, 0, 0, config.itemCounterTextColor);
		}
	}

	/** The item for the fixed id typed into the options, or null if there is no such item. */
	public static Item itemForId(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id.trim());
		return location == null ? null : Registry.ITEM.getOptional(location).orElse(null);
	}

	/** The item being counted, or null if there is nothing to count: no player yet, an empty hand, no item with the fixed id. */
	private static Item trackedItem(ClientConfig config, Player player) {
		if (player == null) {
			return null;
		}
		if (config.itemCounterUseHeldItem) {
			ItemStack held = player.getMainHandItem();
			return held.isEmpty() ? null : held.getItem();
		}
		return itemForId(config.itemCounterItemId);
	}

	/** "Name: count" - named after a plain stack of the item, without a held one's custom name. */
	private static String label(Player player, Item tracked) {
		if (tracked == null) {
			return null;
		}
		return new ItemStack(tracked).getHoverName().getString() + ": " + countInInventory(player, tracked);
	}

	/** How many of the item the player carries: main inventory, hotbar and off hand. Also what the pinned recipes count down against. */
	public static int countInInventory(Player player, Item item) {
		int total = 0;
		for (ItemStack stack : player.inventory.items) {
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
