package com.tntsallin1client.recipe;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.mixin.RecipeBookComponentAccessor;
import com.tntsallin1client.mixin.RecipeBookPageAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5ah, reworked to multi-pin (own user follow-up request): pin whichever recipe is currently
 * hovered in the crafting-table/player-inventory recipe book with a dedicated key, so it shows as
 * a movable HUD reminder ({@link PinnedRecipeHud}) while out gathering materials - not just while
 * the recipe book itself is open. The screens with a recipe book hand out its component themselves
 * ({@link RecipeUpdateListener}); the two accessor mixins ({@link RecipeBookComponentAccessor},
 * {@link RecipeBookPageAccessor}) reach from there to the currently-hovered recipe: the component's
 * page and the page's hovered button are private with no vanilla getter.
 *
 * <p>Pinning takes a one-time snapshot ({@link #buildSnapshot}) rather than keeping the recipe
 * itself, so a pin survives sessions and servers: every ingredient slot is resolved to a concrete
 * item, identical items across slots are summed into one count (a recipe needing 8 cobblestone
 * shows as one "8x Cobblestone" entry, not eight separate icons - the point is "what do I still
 * need", not reproducing the exact crafting-grid arrangement). Each ingredient also gets one
 * level of its own sub-recipe snapshotted alongside it ({@link #findSubIngredients}) - own user
 * request ("wenn ich z.B. nen Repeater crafte das Rezept der Redstone Fackel [mit] anzeigen"),
 * shown only when {@link ClientConfig#pinnedRecipeShowSubIngredients} is on.
 *
 * <p>Only {@link ShapedRecipe}/{@link ShapelessRecipe} are handled - what the crafting-table/
 * inventory recipe book shows besides special recipes without fixed ingredients; anything else
 * (the furnaces' recipe books included) is simply ignored, not an error.
 *
 * <p>Pressing the key again on a recipe that's already pinned unpins it - matched by *content*
 * (same ingredient tally + same result, see {@link #sameRecipe}), own user follow-up request: this
 * way it still works after closing and reopening the recipe book/inventory, not just within the
 * same screen session. {@link #MAX_PINNED} (own user request: "maximal Hauptrezepte 5 Rezepte")
 * caps how many pinned recipes can show on the HUD at once, not how many can be pinned in total -
 * own follow-up request ("die recepys [sollen] ins menü kommen, aber nicht angezeigt werden
 * können"): once {@link #MAX_PINNED} are already visible, pinning another still adds it (so it
 * shows up in {@code PinnedRecipeListScreen}) but starts hidden ({@link PinnedRecipe#visible}
 * {@code = false}) instead of being refused outright - the player frees a slot for it by hiding or
 * unpinning an existing one first. {@link #visibleCount} enforces the same cap wherever a recipe
 * could become visible, including that list screen's own per-entry toggle. An action-bar hint
 * ({@code "pinned_recipe.hidden_hint"}) tells the player this happened, since a pin that doesn't
 * show up on the HUD would otherwise look like it silently failed.
 */
public final class PinnedRecipeManager {
	/** Own user request: "maximal Hauptrezepte 5 Rezepte, das würde ich erstmal damit testen." Caps
	 * how many pinned recipes may be {@link PinnedRecipe#visible} (shown on the HUD) at once - see
	 * this class's own doc comment. */
	public static final int MAX_PINNED = 5;

	private PinnedRecipeManager() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof RecipeUpdateListener)) {
				return;
			}
			ScreenKeyboardEvents.afterKeyPress(screen).register((scr, key, scancode, modifiers) -> {
				if (ModKeyBindings.PIN_RECIPE.matches(key, scancode)) {
					tryTogglePin(client, screen);
				}
			});
			ScreenMouseEvents.afterMouseClick(screen).register((scr, mouseX, mouseY, button) -> {
				if (ModKeyBindings.PIN_RECIPE.matchesMouse(button)) {
					tryTogglePin(client, screen);
				}
			});
		});
	}

	private static void tryTogglePin(Minecraft client, Screen screen) {
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

		PinnedRecipe existing = config.pinnedRecipes.stream().filter(pinned -> sameRecipe(pinned, snapshot)).findFirst().orElse(null);
		if (existing != null) {
			config.pinnedRecipes.remove(existing);
			config.save();
			return;
		}

		snapshot.visible = visibleCount(config.pinnedRecipes) < MAX_PINNED;
		config.pinnedRecipes.add(snapshot);
		config.save();
		if (!snapshot.visible) {
			client.player.displayClientMessage(Component.translatable("gui.tntsallin1client.pinned_recipe.hidden_hint"), true);
		}
	}

	public static long visibleCount(List<PinnedRecipe> pinned) {
		return pinned.stream().filter(recipe -> recipe.visible).count();
	}

	private static boolean sameRecipe(PinnedRecipe a, PinnedRecipe b) {
		if (!a.resultItemId.equals(b.resultItemId) || a.resultCount != b.resultCount) {
			return false;
		}
		return tally(a.ingredients).equals(tally(b.ingredients));
	}

	private static Map<String, Integer> tally(List<PinnedIngredient> ingredients) {
		Map<String, Integer> tally = new LinkedHashMap<>();
		for (PinnedIngredient ingredient : ingredients) {
			tally.put(ingredient.itemId, ingredient.count);
		}
		return tally;
	}

	private static @Nullable RecipeButton hoveredRecipeButton(Screen screen) {
		RecipeBookComponent component = ((RecipeUpdateListener) screen).getRecipeBookComponent();
		if (!component.isVisible()) {
			return null;
		}
		RecipeBookPage page = ((RecipeBookComponentAccessor) component).tntsallin1client$getRecipeBookPage();
		return ((RecipeBookPageAccessor) page).tntsallin1client$getHoveredButton();
	}

	private static @Nullable PinnedRecipe buildSnapshot(Minecraft client, RecipeHolder<?> holder) {
		Recipe<?> shown = holder.value();
		Map<String, Integer> tally = tallyOf(shown, 1);
		if (tally.isEmpty()) {
			return null;
		}
		ItemStack resultStack = shown.getResultItem(client.level.registryAccess());
		if (resultStack.isEmpty()) {
			return null;
		}

		PinnedRecipe recipe = new PinnedRecipe();
		tally.forEach((itemId, count) -> {
			PinnedIngredient ingredient = new PinnedIngredient();
			ingredient.itemId = itemId;
			ingredient.count = count;
			ingredient.subIngredients = findSubIngredients(itemId, count, client);
			recipe.ingredients.add(ingredient);
		});
		recipe.resultItemId = idOf(resultStack.getItem());
		recipe.resultCount = resultStack.getCount();
		return recipe;
	}

	/**
	 * One level down: what {@code neededCount} of {@code itemId} are crafted from. The recipes the
	 * player has unlocked come first (the recipe book's own order), then every crafting recipe there
	 * is - in this version the server hands the client all of them. Empty for a raw material.
	 */
	private static List<PinnedIngredient> findSubIngredients(String itemId, int neededCount, Minecraft client) {
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		if (id == null) {
			return List.of();
		}
		Item item = BuiltInRegistries.ITEM.get(id);
		if (item == Items.AIR) {
			return List.of();
		}

		ClientRecipeBook recipeBook = client.player == null ? null : client.player.getRecipeBook();
		if (recipeBook != null) {
			for (RecipeCollection collection : recipeBook.getCollections()) {
				for (RecipeHolder<?> holder : collection.getRecipes()) {
					List<PinnedIngredient> subIngredients = subIngredientsFrom(holder.value(), item, neededCount, client);
					if (subIngredients != null) {
						return subIngredients;
					}
				}
			}
		}
		for (RecipeHolder<?> holder : client.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			List<PinnedIngredient> subIngredients = subIngredientsFrom(holder.value(), item, neededCount, client);
			if (subIngredients != null) {
				return subIngredients;
			}
		}
		return List.of();
	}

	/** {@code recipe}'s ingredients for {@code neededCount} of {@code item}, or null if it makes something else. */
	private static @Nullable List<PinnedIngredient> subIngredientsFrom(Recipe<?> recipe, Item item, int neededCount, Minecraft client) {
		if (ingredientsOf(recipe).isEmpty()) {
			return null;
		}
		ItemStack result = recipe.getResultItem(client.level.registryAccess());
		if (result.isEmpty() || result.getItem() != item) {
			return null;
		}
		int yieldPerCraft = Math.max(1, result.getCount());
		int craftsNeeded = (neededCount + yieldPerCraft - 1) / yieldPerCraft;

		Map<String, Integer> subTally = tallyOf(recipe, craftsNeeded);
		if (subTally.isEmpty()) {
			return null;
		}
		List<PinnedIngredient> subIngredients = new ArrayList<>();
		subTally.forEach((subItemId, subCount) -> {
			PinnedIngredient sub = new PinnedIngredient();
			sub.itemId = subItemId;
			sub.count = subCount;
			subIngredients.add(sub);
		});
		return subIngredients;
	}

	/** Item id to count over all of {@code recipe}'s ingredient slots, for {@code crafts} rounds of crafting. */
	private static Map<String, Integer> tallyOf(Recipe<?> recipe, int crafts) {
		Map<String, Integer> tally = new LinkedHashMap<>();
		for (Ingredient ingredient : ingredientsOf(recipe)) {
			ItemStack stack = firstStack(ingredient);
			if (stack.isEmpty()) {
				continue;
			}
			tally.merge(idOf(stack.getItem()), stack.getCount() * crafts, Integer::sum);
		}
		return tally;
	}

	private static List<Ingredient> ingredientsOf(Recipe<?> recipe) {
		if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
			return recipe.getIngredients();
		}
		return List.of();
	}

	/** An ingredient that accepts several items (any planks) is shown as its first one, like the recipe book's ghost recipe starts out. */
	private static ItemStack firstStack(Ingredient ingredient) {
		ItemStack[] accepted = ingredient.getItems();
		return accepted.length == 0 ? ItemStack.EMPTY : accepted[0];
	}

	private static String idOf(Item item) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		return id.toString();
	}
}
