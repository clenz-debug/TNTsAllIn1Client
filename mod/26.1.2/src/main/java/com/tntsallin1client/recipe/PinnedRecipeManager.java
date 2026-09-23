package com.tntsallin1client.recipe;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.mixin.AbstractRecipeBookScreenAccessor;
import com.tntsallin1client.mixin.RecipeBookComponentAccessor;
import com.tntsallin1client.mixin.RecipeBookPageAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5ah, reworked to multi-pin (own user follow-up request): pin whichever recipe is currently
 * hovered in the crafting-table/player-inventory recipe book with a dedicated key, so it shows as
 * a movable HUD reminder ({@link PinnedRecipeHud}) while out gathering materials - not just while
 * the recipe book itself is open. See the three accessor mixins ({@link AbstractRecipeBookScreenAccessor},
 * {@link RecipeBookComponentAccessor}, {@link RecipeBookPageAccessor}) for how the currently-hovered
 * recipe is even reached: all three fields involved (the screen's component, the component's page,
 * the page's hovered button) are private with no vanilla getter.
 *
 * <p>{@code RecipeDisplayId} (the client-side recipe identifier) is only a per-connection numeric
 * index, not stable across reconnects/sessions - nothing to persist a live reference to. Pinning
 * instead takes a one-time snapshot ({@link #buildSnapshot}): every ingredient slot is resolved to
 * a concrete item, identical items across slots are summed into one count (a recipe needing 8
 * cobblestone shows as one "8x Cobblestone" entry, not eight separate icons - the point is "what do
 * I still need", not reproducing the exact crafting-grid arrangement). Each ingredient also gets one
 * level of its own sub-recipe snapshotted alongside it ({@link #findSubIngredients}) - own user
 * request ("wenn ich z.B. nen Repeater crafte das Rezept der Redstone Fackel [mit] anzeigen"),
 * shown only when {@link ClientConfig#pinnedRecipeShowSubIngredients} is on.
 *
 * <p>Only {@link ShapedCraftingRecipeDisplay}/{@link ShapelessCraftingRecipeDisplay} are handled -
 * the only two display types the crafting-table/inventory recipe book actually shows (furnace/
 * smithing/stonecutter recipe books are separate screens, not reached through
 * {@link AbstractRecipeBookScreen} at all); any other display type is simply ignored, not an error.
 *
 * <p>Pressing the key again on a recipe that's already pinned unpins it - matched by *content*
 * (same ingredient tally + same result, see {@link #sameRecipe}) rather than the ephemeral
 * {@code RecipeDisplayId} the previous single-pin version tracked, own user follow-up request: this
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
			if (!(screen instanceof AbstractRecipeBookScreen<?>)) {
				return;
			}

			ScreenKeyboardEvents.afterKeyPress(screen).register((scr, keyEvent) -> {
				if (ModKeyBindings.PIN_RECIPE.matches(keyEvent)) {
					tryTogglePin(client, screen);
				}
			});
			ScreenMouseEvents.afterMouseClick(screen).register((scr, mouseEvent, consumed) -> {
				if (ModKeyBindings.PIN_RECIPE.matchesMouse(mouseEvent)) {
					tryTogglePin(client, screen);
				}
				return false;
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

		PinnedRecipe snapshot = buildSnapshot(client, hovered.getCollection(), hovered.getCurrentRecipe());
		if (snapshot == null) {
			return;
		}

		PinnedRecipe existing = config.pinnedRecipes.stream().filter(pinned -> sameRecipe(pinned, snapshot)).findFirst().orElse(null);
		if (existing != null) {
			config.pinnedRecipes.remove(existing);
			config.save();
			return;
		}

		// Own follow-up request: once MAX_PINNED are already shown, a new pin still gets added (so it
		// shows up in PinnedRecipeListScreen) instead of being silently refused - it just starts
		// hidden, since there's no free HUD slot for it yet.
		snapshot.visible = visibleCount(config.pinnedRecipes) < MAX_PINNED;
		config.pinnedRecipes.add(snapshot);
		config.save();

		if (!snapshot.visible) {
			// Own follow-up request: tell the player why the new pin isn't showing up on the HUD,
			// rather than leaving them to notice its absence and wonder if pinning even worked.
			client.player.sendOverlayMessage(Component.translatable("gui.tntsallin1client.pinned_recipe.hidden_hint"));
		}
	}

	/** How many pinned recipes are currently {@link PinnedRecipe#visible} - shared by the auto-pin
	 * cap check above and {@code PinnedRecipeListScreen}'s own per-entry visibility toggle, so the
	 * "at most {@link #MAX_PINNED} shown at once" rule holds no matter how a recipe would become
	 * visible. */
	public static long visibleCount(List<PinnedRecipe> pinned) {
		return pinned.stream().filter(recipe -> recipe.visible).count();
	}

	/** Same recipe by content (result + ingredient tally), not by object identity or the ephemeral
	 * {@code RecipeDisplayId} - see this class's own doc comment for why. */
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
		RecipeBookComponent<?> component = ((AbstractRecipeBookScreenAccessor) screen).tntsallin1client$getRecipeBookComponent();
		if (!component.isVisible()) {
			return null;
		}

		RecipeBookPage page = ((RecipeBookComponentAccessor) component).tntsallin1client$getRecipeBookPage();
		return ((RecipeBookPageAccessor) page).tntsallin1client$getHoveredButton();
	}

	private static @Nullable PinnedRecipe buildSnapshot(Minecraft client, RecipeCollection collection, RecipeDisplayId id) {
		RecipeDisplayEntry entry = collection.getRecipes().stream()
				.filter(candidate -> candidate.id().equals(id))
				.findFirst()
				.orElse(null);
		if (entry == null) {
			return null;
		}

		List<SlotDisplay> ingredientSlots = ingredientsOf(entry.display());
		if (ingredientSlots.isEmpty()) {
			return null;
		}

		ContextMap context = SlotDisplayContext.fromLevel(client.level);
		Map<String, Integer> tally = new LinkedHashMap<>();
		for (SlotDisplay slot : ingredientSlots) {
			ItemStack stack = firstStack(slot, context);
			if (stack.isEmpty()) {
				continue;
			}
			tally.merge(idOf(stack.getItem()), stack.getCount(), Integer::sum);
		}
		if (tally.isEmpty()) {
			return null;
		}

		ItemStack resultStack = firstStack(entry.display().result(), context);
		if (resultStack.isEmpty()) {
			return null;
		}

		ClientRecipeBook recipeBook = client.player.getRecipeBook();
		PinnedRecipe recipe = new PinnedRecipe();
		tally.forEach((itemId, count) -> {
			PinnedIngredient ingredient = new PinnedIngredient();
			ingredient.itemId = itemId;
			ingredient.count = count;
			ingredient.subIngredients = findSubIngredients(itemId, count, recipeBook, context);
			recipe.ingredients.add(ingredient);
		});
		recipe.resultItemId = idOf(resultStack.getItem());
		recipe.resultCount = resultStack.getCount();
		return recipe;
	}

	/**
	 * One level of sub-recipe for a single ingredient item, scaled to how many of it the parent
	 * recipe actually needs - the ingredients of the first known shaped/shapeless recipe that
	 * produces it (searched across every recipe the client currently knows about,
	 * {@link ClientRecipeBook#getCollections()} - not just whatever's shown in the currently-open
	 * recipe book category, since a sub-ingredient's own recipe is very often in a different
	 * category than the recipe being pinned). Empty if the item isn't craftable that way (a raw
	 * material like Redstone Dust) or has no known recipe yet. Deliberately not recursive - this is
	 * only ever called on a top-level ingredient, never on a sub-ingredient's own contents, per user
	 * request to keep it at one level.
	 *
	 * <p>Own follow-up user request: many recipes don't yield 1:1 (3 planks craft into 6 slabs, not
	 * 1) - a naive "one craft's worth of ingredients" would under- or over-state what's actually
	 * needed once the parent needs more than one of the sub-item. Instead this works out how many
	 * *crafts* of the sub-recipe are needed to cover {@code neededCount} (rounded up - you can only
	 * ever craft whole batches, so needing 4 slabs still means doing one full 3-planks-for-6-slabs
	 * craft, not a fractional one) and scales every sub-ingredient by that many crafts.
	 */
	private static List<PinnedIngredient> findSubIngredients(String itemId, int neededCount, ClientRecipeBook recipeBook, ContextMap context) {
		Identifier id = Identifier.tryParse(itemId);
		if (id == null) {
			return List.of();
		}
		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null) {
			return List.of();
		}

		for (RecipeCollection collection : recipeBook.getCollections()) {
			for (RecipeDisplayEntry entry : collection.getRecipes()) {
				List<SlotDisplay> subSlots = ingredientsOf(entry.display());
				if (subSlots.isEmpty()) {
					continue;
				}
				ItemStack result = firstStack(entry.display().result(), context);
				if (result.isEmpty() || result.getItem() != item) {
					continue;
				}

				int yieldPerCraft = Math.max(1, result.getCount());
				int craftsNeeded = (neededCount + yieldPerCraft - 1) / yieldPerCraft;

				Map<String, Integer> subTally = new LinkedHashMap<>();
				for (SlotDisplay slot : subSlots) {
					ItemStack stack = firstStack(slot, context);
					if (stack.isEmpty()) {
						continue;
					}
					subTally.merge(idOf(stack.getItem()), stack.getCount() * craftsNeeded, Integer::sum);
				}
				if (subTally.isEmpty()) {
					continue;
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
		}
		return List.of();
	}

	private static List<SlotDisplay> ingredientsOf(RecipeDisplay display) {
		if (display instanceof ShapedCraftingRecipeDisplay shaped) {
			return shaped.ingredients();
		}
		if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
			return shapeless.ingredients();
		}
		return List.of();
	}

	private static ItemStack firstStack(SlotDisplay slot, ContextMap context) {
		List<ItemStack> resolved = slot.resolveForStacks(context);
		return resolved.isEmpty() ? ItemStack.EMPTY : resolved.get(0);
	}

	private static String idOf(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return id.toString();
	}
}
