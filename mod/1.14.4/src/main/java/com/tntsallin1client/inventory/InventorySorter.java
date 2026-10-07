package com.tntsallin1client.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Reorders the player's main inventory and hotbar (armor, off hand and the crafting grid are left
 * alone). Every move is replayed as the same clicks a player would make with the mouse, so the
 * server's copy of the inventory stays in step instead of drifting apart like after a direct slot write.
 */
public final class InventorySorter {
	// The player's own menu: crafting result and grid 0-4, armor 5-8, main inventory 9-35, hotbar 36-44, off hand 45.
	private static final int FIRST_SLOT = 9;
	/** Exclusive. */
	private static final int LAST_SLOT = 45;
	private static final int LEFT_BUTTON = 0;

	private InventorySorter() {
	}

	public static void sort(Minecraft client) {
		LocalPlayer player = client.player;
		if (player.abilities.instabuild) {
			sortCreative(client, player.inventoryMenu);
			return;
		}
		consolidateStacks(client, player.inventoryMenu);
		reorderByItem(client, player.inventoryMenu);
	}

	/**
	 * Creative mode: the sorted inventory is written slot by slot, the way the creative inventory
	 * itself changes slots (one packet per slot, the server takes it as it is) - no clicks. The
	 * creative screen does not click through the server either, so replayed clicks would run against
	 * what it reports on its own.
	 */
	private static void sortCreative(Minecraft client, AbstractContainerMenu menu) {
		// Same result as the click version: partial stacks merged, then ordered by item (or creative tab first).
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			ItemStack stack = menu.getSlot(slot).getItem();
			if (stack.isEmpty()) {
				continue;
			}
			int remaining = stack.getCount();
			for (ItemStack earlier : stacks) {
				if (remaining == 0) {
					break;
				}
				if (sameKind(earlier, stack)) {
					int moved = Math.min(remaining, earlier.getMaxStackSize() - earlier.getCount());
					earlier.grow(moved);
					remaining -= moved;
				}
			}
			if (remaining > 0) {
				ItemStack rest = stack.copy();
				rest.setCount(remaining);
				stacks.add(rest);
			}
		}
		final Comparator<String> order = keyOrder(stacks);
		Collections.sort(stacks, (first, second) -> order.compare(itemKey(first), itemKey(second)));

		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			int rank = slot - FIRST_SLOT;
			ItemStack sorted = rank < stacks.size() ? stacks.get(rank) : ItemStack.EMPTY;
			if (!ItemStack.matches(menu.getSlot(slot).getItem(), sorted)) {
				menu.getSlot(slot).set(sorted);
				client.gameMode.handleCreativeModeItemAdd(sorted, slot);
			}
		}
	}

	/** Merges scattered partial stacks of the same item into as few slots as possible. */
	private static void consolidateStacks(Minecraft client, AbstractContainerMenu menu) {
		for (int target = FIRST_SLOT; target < LAST_SLOT; target++) {
			ItemStack targetStack = menu.getSlot(target).getItem();
			while (!targetStack.isEmpty() && targetStack.getCount() < targetStack.getMaxStackSize()) {
				int source = findMergeSource(menu, target, targetStack);
				if (source < 0) {
					break;
				}
				int before = targetStack.getCount();
				click(client, menu, source);
				click(client, menu, target);
				if (!client.player.inventory.getCarried().isEmpty()) {
					// Didn't all fit - the rest goes back into the now-empty source slot.
					click(client, menu, source);
				}
				targetStack = menu.getSlot(target).getItem();
				// Should the game not have merged the two after all, trying again would never end.
				if (targetStack.isEmpty() || targetStack.getCount() <= before) {
					break;
				}
			}
		}
	}

	private static int findMergeSource(AbstractContainerMenu menu, int skip, ItemStack target) {
		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			if (slot == skip) {
				continue;
			}
			ItemStack candidate = menu.getSlot(slot).getItem();
			if (!candidate.isEmpty() && sameKind(candidate, target)) {
				return slot;
			}
		}
		return -1;
	}

	/** The game's own rule for putting a clicked stack onto another: same item, same extra data (which holds the wear too). */
	private static boolean sameKind(ItemStack first, ItemStack second) {
		return first.getItem() == second.getItem() && ItemStack.tagMatches(first, second);
	}

	/**
	 * Groups the now-consolidated stacks together at the front, sorted by item id - or, with
	 * {@link ClientConfig#quickSortGroupByCategory}, first by the item's creative inventory tab
	 * (Building Blocks, Decoration Blocks, ... in the tabs' own order) and by item id within each.
	 */
	private static void reorderByItem(Minecraft client, AbstractContainerMenu menu) {
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
			ItemStack stack = menu.getSlot(slot).getItem();
			if (!stack.isEmpty()) {
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
			if (itemKey(menu.getSlot(targetPos).getItem()).equals(wantKey)) {
				continue;
			}
			int sourcePos = findFirstWithKey(menu, targetPos, wantKey);
			if (sourcePos < 0) {
				// The inventory changed under the sort (the server put something in) - leave the rest as it is.
				return;
			}
			swap(client, menu, targetPos, sourcePos);
		}
	}

	private static int findFirstWithKey(AbstractContainerMenu menu, int from, String key) {
		for (int slot = from; slot < LAST_SLOT; slot++) {
			if (itemKey(menu.getSlot(slot).getItem()).equals(key)) {
				return slot;
			}
		}
		return -1;
	}

	private static void swap(Minecraft client, AbstractContainerMenu menu, int first, int second) {
		if (first == second) {
			return;
		}
		click(client, menu, second);
		click(client, menu, first);
		if (!client.player.inventory.getCarried().isEmpty()) {
			click(client, menu, second);
		}
	}

	/** What a stack is sorted by: its item's id; empty for no stack. */
	private static String itemKey(ItemStack stack) {
		return stack.isEmpty() ? "" : String.valueOf(Registry.ITEM.getKey(stack.getItem()));
	}

	/** By key alone, or by the item's creative tab first where the player chose that. */
	private static Comparator<String> keyOrder(List<ItemStack> stacks) {
		final Comparator<String> byName = Comparator.naturalOrder();
		if (!ClientConfig.get().quickSortGroupByCategory) {
			return byName;
		}
		final Map<String, Integer> tabRanks = new HashMap<String, Integer>();
		for (ItemStack stack : stacks) {
			CreativeModeTab tab = stack.getItem().getItemCategory();
			// An item in no tab comes after all tabs.
			tabRanks.put(itemKey(stack), tab != null ? tab.getId() : CreativeModeTab.TABS.length);
		}
		return (first, second) -> {
			int byTab = Integer.compare(tabRanks.get(first), tabRanks.get(second));
			return byTab != 0 ? byTab : byName.compare(first, second);
		};
	}

	private static void click(Minecraft client, AbstractContainerMenu menu, int slot) {
		client.gameMode.handleInventoryMouseClick(menu.containerId, slot, LEFT_BUTTON, ClickType.PICKUP, client.player);
	}
}
