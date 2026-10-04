package com.tntsallin1client.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.screen.ScreenHandler;

/**
 * Reorders the player's main inventory and hotbar (armor and the crafting grid are left alone).
 * Every move is replayed as the same clicks a player would make with the mouse, so the server's
 * copy of the inventory stays in step instead of drifting apart like after a direct slot write.
 */
public final class InventorySorter {
	// The player's own screen handler: crafting result and grid 0-4, armor 5-8, main inventory 9-35, hotbar 36-44.
	private static final int FIRST_SLOT = 9;
	/** Exclusive. */
	private static final int LAST_SLOT = 45;
	private static final int LEFT_BUTTON = 0;
	private static final int PICKUP_MODE = 0;

	private InventorySorter() {
	}

	public static void sort(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player.abilities.creativeMode) {
			sortCreative(client, player.playerScreenHandler);
			return;
		}
		consolidateStacks(client, player.playerScreenHandler);
		reorderByItem(client, player.playerScreenHandler);
	}

	/**
	 * Creative mode: the sorted inventory is written slot by slot, the way the creative inventory
	 * itself changes slots (one packet per slot, the server takes it as it is) - no clicks. The
	 * creative screen does not click through the server either, so replayed clicks would run against
	 * what it reports on its own.
	 */
	private static void sortCreative(MinecraftClient client, ScreenHandler handler) {
		// Same result as the click version: partial stacks merged, then ordered by item (or item group first).
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			ItemStack stack = handler.getSlot(slot).getStack();
			if (stack == null) {
				continue;
			}
			int remaining = stack.count;
			for (ItemStack earlier : stacks) {
				if (remaining == 0) {
					break;
				}
				if (sameKind(earlier, stack)) {
					int moved = Math.min(remaining, earlier.getMaxCount() - earlier.count);
					earlier.count += moved;
					remaining -= moved;
				}
			}
			if (remaining > 0) {
				ItemStack rest = stack.copy();
				rest.count = remaining;
				stacks.add(rest);
			}
		}

		final Comparator<String> order = keyOrder(stacks);
		Collections.sort(stacks, (first, second) -> order.compare(itemKey(first), itemKey(second)));

		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			int rank = slot - FIRST_SLOT;
			ItemStack sorted = rank < stacks.size() ? stacks.get(rank) : null;
			if (!ItemStack.equalsAll(handler.getSlot(slot).getStack(), sorted)) {
				handler.getSlot(slot).setStack(sorted);
				client.interactionManager.clickCreativeStack(sorted, slot);
			}
		}
	}

	/** Merges scattered partial stacks of the same item into as few slots as possible. */
	private static void consolidateStacks(MinecraftClient client, ScreenHandler handler) {
		for (int target = FIRST_SLOT; target < LAST_SLOT; target++) {
			ItemStack targetStack = handler.getSlot(target).getStack();
			while (targetStack != null && targetStack.count < targetStack.getMaxCount()) {
				int source = findMergeSource(handler, target, targetStack);
				if (source < 0) {
					break;
				}
				int before = targetStack.count;
				click(client, handler, source);
				click(client, handler, target);
				if (client.player.inventory.getCursorStack() != null) {
					// Didn't all fit - the rest goes back into the now-empty source slot.
					click(client, handler, source);
				}
				targetStack = handler.getSlot(target).getStack();
				// Should the game not have merged the two after all, trying again would never end.
				if (targetStack == null || targetStack.count <= before) {
					break;
				}
			}
		}
	}

	private static int findMergeSource(ScreenHandler handler, int skip, ItemStack target) {
		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			if (slot == skip) {
				continue;
			}
			ItemStack candidate = handler.getSlot(slot).getStack();
			if (candidate != null && sameKind(candidate, target)) {
				return slot;
			}
		}
		return -1;
	}

	/**
	 * The game's own rule for putting a clicked stack onto another (bytecode-checked in
	 * `ScreenHandler#onSlotClick`): same item, same damage value, same extra data. Legacy Yarn calls
	 * the comparison of the extra data `equalsIgnoreDamage`; it compares nothing else.
	 */
	private static boolean sameKind(ItemStack first, ItemStack second) {
		return first.getItem() == second.getItem() && first.getData() == second.getData() && ItemStack.equalsIgnoreDamage(first, second);
	}

	/**
	 * Groups the now-consolidated stacks together at the front, sorted by item id - or, with
	 * {@link ClientConfig#quickSortGroupByCategory}, first by the item's creative inventory tab
	 * (Building Blocks, Decoration Blocks, ... in the tabs' own order) and by item id within each.
	 */
	private static void reorderByItem(MinecraftClient client, ScreenHandler handler) {
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			ItemStack stack = handler.getSlot(slot).getStack();
			if (stack != null) {
				stacks.add(stack);
			}
		}
		Comparator<String> order = keyOrder(stacks);
		List<String> targetKeys = new ArrayList<String>();
		for (ItemStack stack : stacks) {
			targetKeys.add(itemKey(stack));
		}
		Collections.sort(targetKeys, order);

		for (int rank = 0; rank < targetKeys.size(); rank++) {
			int targetPos = FIRST_SLOT + rank;
			String wantKey = targetKeys.get(rank);
			if (itemKey(handler.getSlot(targetPos).getStack()).equals(wantKey)) {
				continue;
			}
			int sourcePos = findFirstWithKey(handler, targetPos, wantKey);
			if (sourcePos < 0) {
				// The inventory changed under the sort (the server put something in) - leave the rest as it is.
				return;
			}
			swap(client, handler, targetPos, sourcePos);
		}
	}

	private static int findFirstWithKey(ScreenHandler handler, int from, String key) {
		for (int slot = from; slot < LAST_SLOT; slot++) {
			if (itemKey(handler.getSlot(slot).getStack()).equals(key)) {
				return slot;
			}
		}
		return -1;
	}

	private static void swap(MinecraftClient client, ScreenHandler handler, int first, int second) {
		if (first == second) {
			return;
		}
		click(client, handler, second);
		click(client, handler, first);
		if (client.player.inventory.getCursorStack() != null) {
			click(client, handler, second);
		}
	}

	/** What a stack is sorted by; empty for no stack. */
	private static String itemKey(ItemStack stack) {
		if (stack == null) {
			return "";
		}
		Item item = stack.getItem();
		String id = String.valueOf(Item.REGISTRY.getIdentifier(item));
		// The damage value of an item that can't wear down names a variant of it (wool colors, wood types).
		return item.isDamageable() ? id : id + String.format("#%05d", stack.getData());
	}

	/** By key alone, or by the item's creative tab first where the player chose that. */
	private static Comparator<String> keyOrder(List<ItemStack> stacks) {
		final Comparator<String> byName = Comparator.naturalOrder();
		if (!ClientConfig.get().quickSortGroupByCategory) {
			return byName;
		}
		final Map<String, Integer> tabRanks = new HashMap<String, Integer>();
		for (ItemStack stack : stacks) {
			ItemGroup group = stack.getItem().getItemGroup();
			// An item in no tab comes after all tabs.
			tabRanks.put(itemKey(stack), group != null ? group.getIndex() : ItemGroup.itemGroups.length);
		}
		return (first, second) -> {
			int byTab = Integer.compare(tabRanks.get(first), tabRanks.get(second));
			return byTab != 0 ? byTab : byName.compare(first, second);
		};
	}

	private static void click(MinecraftClient client, ScreenHandler handler, int slot) {
		client.interactionManager.clickSlot(handler.syncId, slot, LEFT_BUTTON, PICKUP_MODE, client.player);
	}
}
