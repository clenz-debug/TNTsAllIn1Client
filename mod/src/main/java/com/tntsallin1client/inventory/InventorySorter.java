package com.tntsallin1client.inventory;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5c: reorders the player's main inventory + hotbar (armor and offhand are
 * left alone). Every move is replayed as the same PICKUP click sequence a human
 * clicking with the mouse would send, so the server's copy of the inventory stays
 * in sync instead of desyncing like a direct slot write would.
 */
public final class InventorySorter {
	private static final int FIRST_SLOT = InventoryMenu.INV_SLOT_START; // 9
	private static final int LAST_SLOT = InventoryMenu.USE_ROW_SLOT_END; // 45 (exclusive): main inv + hotbar

	private InventorySorter() {
	}

	public static void sort(MultiPlayerGameMode gameMode, InventoryMenu menu, Player player) {
		consolidateStacks(gameMode, menu, player);
		reorderByItem(gameMode, menu, player);
	}

	/** Merges scattered partial stacks of the same item into as few slots as possible. */
	private static void consolidateStacks(MultiPlayerGameMode gameMode, InventoryMenu menu, Player player) {
		for (int target = FIRST_SLOT; target < LAST_SLOT; target++) {
			ItemStack targetStack = menu.getSlot(target).getItem();
			while (!targetStack.isEmpty() && targetStack.getCount() < targetStack.getMaxStackSize()) {
				int source = findMergeSource(menu, target, targetStack);
				if (source < 0) {
					break;
				}
				click(gameMode, menu, player, source, ClickType.PICKUP);
				click(gameMode, menu, player, target, ClickType.PICKUP);
				if (!menu.getCarried().isEmpty()) {
					// Didn't all fit - the rest goes back into the now-empty source slot.
					click(gameMode, menu, player, source, ClickType.PICKUP);
				}
				targetStack = menu.getSlot(target).getItem();
			}
		}
	}

	private static int findMergeSource(InventoryMenu menu, int skip, ItemStack target) {
		for (int i = FIRST_SLOT; i < LAST_SLOT; i++) {
			if (i == skip) {
				continue;
			}
			ItemStack candidate = menu.getSlot(i).getItem();
			if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, target)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Groups the now-consolidated stacks together at the front, sorted by item id -
	 * or, with {@link ClientConfig#quickSortGroupByCategory}, first by the item's
	 * creative-inventory category (Building Blocks, Redstone Blocks, ... same order
	 * as the creative tab row) and by item id within each category.
	 */
	private static void reorderByItem(MultiPlayerGameMode gameMode, InventoryMenu menu, Player player) {
		List<String> targetKeys = new ArrayList<>();
		for (int i = FIRST_SLOT; i < LAST_SLOT; i++) {
			ItemStack stack = menu.getSlot(i).getItem();
			if (!stack.isEmpty()) {
				targetKeys.add(itemKey(stack));
			}
		}

		if (ClientConfig.get().quickSortGroupByCategory) {
			Map<String, Integer> categoryRanks = categoryRanks(targetKeys);
			targetKeys.sort(Comparator.<String>comparingInt(categoryRanks::get).thenComparing(Comparator.naturalOrder()));
		} else {
			targetKeys.sort(String::compareTo);
		}

		for (int rank = 0; rank < targetKeys.size(); rank++) {
			int targetPos = FIRST_SLOT + rank;
			String wantKey = targetKeys.get(rank);
			if (itemKey(menu.getSlot(targetPos).getItem()).equals(wantKey)) {
				continue;
			}
			int sourcePos = findFirstWithKey(menu, targetPos, wantKey);
			swap(gameMode, menu, player, targetPos, sourcePos);
		}
	}

	private static int findFirstWithKey(InventoryMenu menu, int from, String key) {
		for (int i = from; i < LAST_SLOT; i++) {
			if (itemKey(menu.getSlot(i).getItem()).equals(key)) {
				return i;
			}
		}
		throw new IllegalStateException("Sorted item key not found while reordering: " + key);
	}

	private static void swap(MultiPlayerGameMode gameMode, InventoryMenu menu, Player player, int a, int b) {
		if (a == b) {
			return;
		}
		if (menu.getSlot(a).getItem().isEmpty() && menu.getSlot(b).getItem().isEmpty()) {
			return;
		}
		click(gameMode, menu, player, a, ClickType.PICKUP);
		click(gameMode, menu, player, b, ClickType.PICKUP);
		if (!menu.getCarried().isEmpty()) {
			click(gameMode, menu, player, a, ClickType.PICKUP);
		}
	}

	private static String itemKey(ItemStack stack) {
		return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	/** One rank per distinct item key - computed once so repeated keys (stacks split across slots) aren't looked up twice. */
	private static Map<String, Integer> categoryRanks(List<String> keys) {
		List<CreativeModeTab> categories = CreativeModeTabs.allTabs().stream()
				.filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
				.toList();
		Map<String, Integer> ranks = new HashMap<>();
		for (String key : keys) {
			ranks.computeIfAbsent(key, k -> categoryRankOf(categories, k));
		}
		return ranks;
	}

	/** Index of the first creative-tab category (Building Blocks, Redstone Blocks, ...) the item belongs to. */
	private static int categoryRankOf(List<CreativeModeTab> categories, String key) {
		Identifier id = Identifier.tryParse(key);
		Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
		if (item == null) {
			return categories.size();
		}

		ItemStack stack = new ItemStack(item);
		for (int i = 0; i < categories.size(); i++) {
			if (categories.get(i).contains(stack)) {
				return i;
			}
		}
		return categories.size();
	}

	private static void click(MultiPlayerGameMode gameMode, InventoryMenu menu, Player player, int slot, ClickType type) {
		gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, type, player);
	}
}
