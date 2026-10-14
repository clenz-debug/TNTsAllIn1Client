package com.tntsallin1client.recipe;

import java.util.ArrayList;
import java.util.List;

/** One row of {@link PinnedRecipe}'s ingredient list - a single item id and how many of it the
 * recipe needs in total (identical ingredient slots across the recipe are summed into one entry
 * rather than shown as repeated icons, see {@link PinnedRecipeManager}). */
public class PinnedIngredient {
	public String itemId = "minecraft:air";
	public int count = 1;
	/** One level of this ingredient's own sub-recipe - the ingredients of whichever known shaped/
	 * shapeless recipe produces this item, snapshotted at pin time same as everything else in
	 * {@link PinnedRecipe} (own user request: "wenn ich z.B. nen Repeater crafte das Rezept der
	 * Redstone Fackel [mit] anzeigen"). Empty if this item isn't craftable that way (a raw material
	 * like Redstone Dust) - only ever populated one level deep, entries in here never have their own
	 * sub-ingredients (deliberately not recursive, per user request to keep it at one level). Shown
	 * only when {@code ClientConfig#pinnedRecipeShowSubIngredients} is on; always snapshotted
	 * regardless so toggling that setting later doesn't require re-pinning. */
	public List<PinnedIngredient> subIngredients = new ArrayList<>();
}
