package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Total count of one item across the player's inventory - either a fixed item or whatever is held
 * right now, per {@link ClientConfig#itemCounterUseHeldItem}. Sits at the right edge below the other
 * displays there until moved.
 *
 * <p>Items that come in variants sharing one id in this version of the game (wool colors, dyes, wood
 * types): the held item counts only its own variant; a fixed item, given by its id alone, counts
 * all of them.
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
	protected boolean isHidden(MinecraftClient client) {
		return client.options.debugEnabled;
	}

	@Override
	public int width(MinecraftClient client) {
		ClientConfig config = ClientConfig.get();
		String label = label(config, trackedStack(config, client.player));
		if (label == null) {
			return 0;
		}
		int textWidth = client.textRenderer.getStringWidth(label);
		return config.itemCounterShowItemIcon ? ItemIcons.SIZE + ICON_GAP + textWidth : textWidth;
	}

	@Override
	public int height(MinecraftClient client) {
		ClientConfig config = ClientConfig.get();
		if (trackedStack(config, client.player) == null) {
			return 0;
		}
		return config.itemCounterShowItemIcon ? Math.max(ItemIcons.SIZE, client.textRenderer.fontHeight) : client.textRenderer.fontHeight;
	}

	@Override
	public int defaultX(MinecraftClient client, int screenWidth, int screenHeight) {
		return screenWidth - EDGE_MARGIN - width(client);
	}

	@Override
	public int defaultY(MinecraftClient client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}

	@Override
	protected void draw(MinecraftClient client, ClientConfig config) {
		ItemStack tracked = trackedStack(config, client.player);
		String label = label(config, tracked);
		if (label == null) {
			return;
		}
		if (config.itemCounterShowItemIcon) {
			ItemIcons.draw(client, tracked, 0, 0);
			client.textRenderer.drawWithShadow(label, ItemIcons.SIZE + ICON_GAP, (ItemIcons.SIZE - client.textRenderer.fontHeight) / 2, config.itemCounterTextColor);
		} else {
			client.textRenderer.drawWithShadow(label, 0, 0, config.itemCounterTextColor);
		}
	}

	/** The item for the fixed id typed into the options, or null if there is no such item. */
	public static Item itemForId(String id) {
		try {
			return Item.getFromId(id.trim());
		} catch (RuntimeException e) {
			// Not something an item id can look like at all.
			return null;
		}
	}

	/**
	 * One item of the kind being counted - what the label is named after and the icon shows - or null
	 * if there is nothing to count: no player yet, an empty hand, no item with the fixed id.
	 */
	private static ItemStack trackedStack(ClientConfig config, PlayerEntity player) {
		if (player == null) {
			return null;
		}
		if (config.itemCounterUseHeldItem) {
			ItemStack held = player.inventory.getMainHandStack();
			if (held == null) {
				return null;
			}
			// A stack of its own - without the held one's custom name, enchantments or wear.
			return new ItemStack(held.getItem(), 1, hasVariants(held.getItem()) ? held.getDamage() : 0);
		}
		Item item = itemForId(config.itemCounterItemId);
		return item == null ? null : new ItemStack(item);
	}

	/**
	 * Whether a stack's damage value says which variant of the item it is (tall grass or shrub, the
	 * wool color) rather than how worn it is. Everything that can't wear out is treated that way - for
	 * an item without variants the value is always 0 and the comparison changes nothing. (The method
	 * this name table calls `Item#hasSubTypes` is not that question - it answered false for tall
	 * grass, which made the counter show the shrub.)
	 */
	private static boolean hasVariants(Item item) {
		return !item.isDamageable();
	}

	private String label(ClientConfig config, ItemStack tracked) {
		if (tracked == null) {
			return null;
		}
		PlayerEntity player = MinecraftClient.getInstance().player;
		// Only the held item is one particular variant.
		boolean sameVariantOnly = config.itemCounterUseHeldItem && hasVariants(tracked.getItem());
		int total = 0;
		for (ItemStack stack : player.inventory.main) {
			if (stack != null && stack.getItem() == tracked.getItem() && (!sameVariantOnly || stack.getDamage() == tracked.getDamage())) {
				total += stack.count;
			}
		}
		return tracked.getCustomName() + ": " + total;
	}
}
