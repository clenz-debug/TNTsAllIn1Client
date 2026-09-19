package com.tntsallin1client.recipe;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.mixin.AbstractRecipeBookScreenAccessor;
import com.tntsallin1client.mixin.RecipeBookComponentAccessor;
import com.tntsallin1client.mixin.RecipeBookPageAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5ah: pin whichever recipe is currently hovered in the crafting-table/player-inventory
 * recipe book with a dedicated key, so it shows as a movable HUD reminder ({@link PinnedRecipeHud})
 * while out gathering materials - not just while the recipe book itself is open. See the three
 * accessor mixins ({@link AbstractRecipeBookScreenAccessor}, {@link RecipeBookComponentAccessor},
 * {@link RecipeBookPageAccessor}) for how the currently-hovered recipe is even reached: all three
 * fields involved (the screen's component, the component's page, the page's hovered button) are
 * private with no vanilla getter.
 *
 * <p>{@code RecipeDisplayId} (the client-side recipe identifier) is only a per-connection numeric
 * index, not stable across reconnects/sessions - nothing to persist a live reference to. Pinning
 * instead takes a one-time snapshot ({@link #buildSnapshot}): every ingredient slot is resolved to
 * a concrete item, identical items across slots are summed into one count (a recipe needing 8
 * cobblestone shows as one "8x Cobblestone" entry, not eight separate icons - the point is "what do
 * I still need", not reproducing the exact crafting-grid arrangement).
 *
 * <p>Only {@link ShapedCraftingRecipeDisplay}/{@link ShapelessCraftingRecipeDisplay} are handled -
 * the only two display types the crafting-table/inventory recipe book actually shows (furnace/
 * smithing/stonecutter recipe books are separate screens, not reached through
 * {@link AbstractRecipeBookScreen} at all); any other display type is simply ignored, not an error.
 *
 * <p>Pressing the key again on the exact same recipe unpins it (tracked via a session-only,
 * non-persisted {@code currentlyPinnedDisplayId} - the numeric id is stable within one session,
 * just not across a restart, so this is only ever a same-session convenience, not the source of
 * truth for whether something is pinned).
 */
public final class PinnedRecipeManager {
	private static @Nullable RecipeDisplayId currentlyPinnedDisplayId;

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
		if (!config.pinnedRecipeEnabled || client.level == null) {
			return;
		}

		RecipeButton hovered = hoveredRecipeButton(screen);
		if (hovered == null) {
			return;
		}

		RecipeDisplayId id = hovered.getCurrentRecipe();
		if (id.equals(currentlyPinnedDisplayId)) {
			config.pinnedRecipe = null;
			currentlyPinnedDisplayId = null;
			config.save();
			return;
		}

		PinnedRecipe pinned = buildSnapshot(client, hovered.getCollection(), id);
		if (pinned == null) {
			return;
		}

		config.pinnedRecipe = pinned;
		currentlyPinnedDisplayId = id;
		config.save();
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

		PinnedRecipe recipe = new PinnedRecipe();
		tally.forEach((itemId, count) -> {
			PinnedIngredient ingredient = new PinnedIngredient();
			ingredient.itemId = itemId;
			ingredient.count = count;
			recipe.ingredients.add(ingredient);
		});
		recipe.resultItemId = idOf(resultStack.getItem());
		recipe.resultCount = resultStack.getCount();
		return recipe;
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
