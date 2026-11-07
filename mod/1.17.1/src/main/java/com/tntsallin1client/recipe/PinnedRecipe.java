package com.tntsallin1client.recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * A snapshot of a crafting recipe's ingredients and result, taken once at pin time - not a live
 * reference back to the recipe system (the client-side {@code RecipeDisplayId} that identifies a
 * recipe is only a per-connection numeric index, not stable across sessions/reconnects, so there
 * is nothing stable to hold onto instead). Deliberately just item ids + counts, not exact grid
 * positions - the point is "what do I still need to gather", which a compact ingredient summary
 * answers better than reproducing the crafting grid shape would.
 */
public class PinnedRecipe {
	/** Own follow-up user request ("die Menge an Items die man vom gepinnten Rezept craften will
	 * anzeigen können") - upper bound for {@link #craftMultiplier}, adjusted from
	 * {@code PinnedRecipeListScreen}'s own +/- stepper or typed directly into its quantity field
	 * (own further follow-up request, "auch über ein Feld eintippbar machen, wenn man z.B. auch mehr
	 * als 64 Items braucht" - the stepper alone originally capped at 64). Just a sane UI ceiling well
	 * clear of any int-overflow risk once multiplied through ingredient counts, not tied to any
	 * actual item stack limit (a fake, detached display {@code ItemStack} doesn't clamp its count). */
	public static final int MAX_CRAFT_MULTIPLIER = 9999;

	public List<PinnedIngredient> ingredients = new ArrayList<>();
	public String resultItemId = "minecraft:air";
	public int resultCount = 1;
	/** Per-entry show/hide, independent of unpinning it entirely - same shape as {@code Waypoint#visible}
	 * (own user request: "wie quasi das Waypoint-Menü"), toggled from {@code PinnedRecipeListScreen}.
	 * Starts {@code false} when pinned while {@link PinnedRecipeManager#MAX_PINNED} others are already
	 * visible - see that class's own doc comment. */
	public boolean visible = true;
	/** How many times the player intends to craft this recipe - scales every ingredient and the
	 * result count shown for it ({@code PinnedRecipeHud}), independent of the recipe's own per-craft
	 * yield ({@link #resultCount}). Own follow-up user request, adjusted from
	 * {@code PinnedRecipeListScreen}'s own +/- stepper; deliberately not part of {@code sameRecipe}'s
	 * content match in {@code PinnedRecipeManager} - it's a display preference, not part of what
	 * makes two pins "the same recipe". */
	public int craftMultiplier = 1;
}
