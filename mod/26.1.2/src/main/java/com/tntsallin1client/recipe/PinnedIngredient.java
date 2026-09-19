package com.tntsallin1client.recipe;

/** One row of {@link PinnedRecipe}'s ingredient list - a single item id and how many of it the
 * recipe needs in total (identical ingredient slots across the recipe are summed into one entry
 * rather than shown as repeated icons, see {@link PinnedRecipeManager}). */
public class PinnedIngredient {
	public String itemId = "minecraft:air";
	public int count = 1;
}
