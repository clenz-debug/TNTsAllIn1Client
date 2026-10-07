package com.tntsallin1client.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.mixin.RecipeBookComponentAccessor;
import com.tntsallin1client.mixin.RecipeBookPageAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Pinned recipes: a key pins whichever recipe is hovered in the recipe book of the crafting table or
 * the inventory, so it shows as a movable HUD reminder ({@link PinnedRecipeHud}) while out gathering
 * materials. Pressing the key on a recipe that is pinned already unpins it.
 *
 * <p>A pin is a snapshot ({@link PinnedRecipe}): every ingredient slot is taken as its first
 * possible item, the same items across slots are summed into one count (a recipe needing 8
 * cobblestone is one "8x Cobblestone", not eight icons - the point is "what do I still need", not
 * the shape in the grid). Each ingredient also gets one level of its own recipe alongside it
 * ({@link #findSubIngredients}), shown only where the player switched that on.
 *
 * <p>Only crafting recipes with fixed ingredients are handled - what the two recipe books in
 * question show. A recipe the game works out from what is put in (firework rockets, dyed armor) has
 * no ingredient list to snapshot.
 *
 * <p>{@link #MAX_PINNED} caps how many pinned recipes show on the HUD at once, not how many can be
 * pinned: once that many are visible, another pin is still added (and appears in the list screen)
 * but starts hidden, and a hint in the action bar says so - a pin that doesn't show up would
 * otherwise look like it failed.
 */
public final class PinnedRecipeManager {
	/** How many pinned recipes may be {@link PinnedRecipe#visible} at once. */
	public static final int MAX_PINNED = 5;

	private PinnedRecipeManager() {
	}

	/** The pin key was pressed with `screen` open. Nothing happens unless a recipe in its recipe book is under the cursor. */
	public static void togglePin(Minecraft client, Screen screen) {
		ClientConfig config = ClientConfig.get();
		if (!config.pinnedRecipeEnabled || client.level == null || client.player == null) {
			return;
		}
		RecipeButton hovered = hoveredRecipeButton(screen);
		if (hovered == null) {
			return;
		}
		PinnedRecipe snapshot = buildSnapshot(client, hovered.getRecipe());
		if (snapshot == null) {
			return;
		}

		for (PinnedRecipe pinned : config.pinnedRecipes) {
			if (sameRecipe(pinned, snapshot)) {
				config.pinnedRecipes.remove(pinned);
				config.save();
				return;
			}
		}

		snapshot.visible = visibleCount(config.pinnedRecipes) < MAX_PINNED;
		config.pinnedRecipes.add(snapshot);
		config.save();
		if (!snapshot.visible) {
			client.gui.setOverlayMessage(new TextComponent(I18n.get("gui.tntsallin1client.pinned_recipe.hidden_hint")), false);
		}
	}

	/** How many pinned recipes are {@link PinnedRecipe#visible} - asked wherever one could become visible, so the cap holds everywhere. */
	public static int visibleCount(List<PinnedRecipe> pinned) {
		int count = 0;
		for (PinnedRecipe recipe : pinned) {
			if (recipe.visible) {
				count++;
			}
		}
		return count;
	}

	/** The same recipe by what it is - result and ingredients - so a recipe is still recognized after the recipe book was closed and reopened. */
	private static boolean sameRecipe(PinnedRecipe first, PinnedRecipe second) {
		if (!first.resultItemId.equals(second.resultItemId) || first.resultCount != second.resultCount) {
			return false;
		}
		return tally(first.ingredients).equals(tally(second.ingredients));
	}

	private static Map<String, Integer> tally(List<PinnedIngredient> ingredients) {
		Map<String, Integer> tally = new LinkedHashMap<String, Integer>();
		for (PinnedIngredient ingredient : ingredients) {
			tally.put(ingredient.itemId, ingredient.count);
		}
		return tally;
	}

	/** The recipe button under the cursor - the screens with a recipe book hand the book out, the rest is private in it. */
	private static RecipeButton hoveredRecipeButton(Screen screen) {
		if (!(screen instanceof RecipeUpdateListener)) {
			return null;
		}
		RecipeBookComponent component = ((RecipeUpdateListener) screen).getRecipeBookComponent();
		if (component == null || !component.isVisible()) {
			return null;
		}
		RecipeBookPage page = ((RecipeBookComponentAccessor) component).tntsallin1client$getRecipeBookPage();
		return ((RecipeBookPageAccessor) page).tntsallin1client$getHoveredButton();
	}

	private static PinnedRecipe buildSnapshot(Minecraft client, Recipe<?> recipe) {
		if (!isPlainCraftingRecipe(recipe)) {
			return null;
		}
		Map<String, Integer> tally = ingredientTally(recipe, 1);
		ItemStack result = recipe.getResultItem();
		if (tally.isEmpty() || result.isEmpty()) {
			return null;
		}
		PinnedRecipe pinned = new PinnedRecipe();
		for (Map.Entry<String, Integer> entry : tally.entrySet()) {
			PinnedIngredient ingredient = new PinnedIngredient();
			ingredient.itemId = entry.getKey();
			ingredient.count = entry.getValue();
			ingredient.subIngredients = findSubIngredients(client, entry.getKey(), entry.getValue());
			pinned.ingredients.add(ingredient);
		}
		pinned.resultItemId = idOf(result.getItem());
		pinned.resultCount = result.getCount();
		return pinned;
	}

	private static boolean isPlainCraftingRecipe(Recipe<?> recipe) {
		return recipe != null && recipe.getType() == RecipeType.CRAFTING && !recipe.isSpecial();
	}

	/** Item id to how many of it the recipe needs for `crafts` rounds, in the order the grid has them. */
	private static Map<String, Integer> ingredientTally(Recipe<?> recipe, int crafts) {
		Map<String, Integer> tally = new LinkedHashMap<String, Integer>();
		for (Ingredient ingredient : recipe.getIngredients()) {
			// An empty slot of the grid has no items; a slot that takes any of several takes the first.
			ItemStack[] choices = ingredient.getItems();
			if (choices.length == 0 || choices[0].isEmpty()) {
				continue;
			}
			String id = idOf(choices[0].getItem());
			Integer before = tally.get(id);
			tally.put(id, (before == null ? 0 : before) + crafts);
		}
		return tally;
	}

	/**
	 * One level of recipe for a single ingredient, scaled to how many of it the pinned recipe needs:
	 * the ingredients of a crafting recipe that makes the item. Empty for a raw material. Never goes
	 * deeper than this one level (own user request in the newer versions).
	 *
	 * <p>Many recipes don't give one item per craft (3 planks make 6 slabs): what is needed is worked
	 * out in whole crafts, rounded up, and the ingredients scaled by that.
	 *
	 * <p>The server sends this version's client every recipe there is, unlocked or not, so they are
	 * all searched. Where several recipes make the item, the one whose name comes first is taken -
	 * the plain one ("stick") rather than a variant ("stick_from_bamboo_item"), and always the same.
	 */
	private static List<PinnedIngredient> findSubIngredients(Minecraft client, String itemId, int neededCount) {
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		Item item = id == null ? null : Registry.ITEM.getOptional(id).orElse(null);
		if (item == null || client.getConnection() == null) {
			return Collections.emptyList();
		}
		Recipe<?> best = null;
		for (Recipe<?> candidate : client.getConnection().getRecipeManager().getRecipes()) {
			if (!isPlainCraftingRecipe(candidate) || candidate.getResultItem().getItem() != item) {
				continue;
			}
			if (best == null || candidate.getId().toString().compareTo(best.getId().toString()) < 0) {
				best = candidate;
			}
		}
		if (best == null) {
			return Collections.emptyList();
		}
		int yieldPerCraft = Math.max(1, best.getResultItem().getCount());
		int craftsNeeded = (neededCount + yieldPerCraft - 1) / yieldPerCraft;
		List<PinnedIngredient> subIngredients = new ArrayList<PinnedIngredient>();
		for (Map.Entry<String, Integer> entry : ingredientTally(best, craftsNeeded).entrySet()) {
			PinnedIngredient sub = new PinnedIngredient();
			sub.itemId = entry.getKey();
			sub.count = entry.getValue();
			subIngredients.add(sub);
		}
		return subIngredients;
	}

	private static String idOf(Item item) {
		return String.valueOf(Registry.ITEM.getKey(item));
	}
}
