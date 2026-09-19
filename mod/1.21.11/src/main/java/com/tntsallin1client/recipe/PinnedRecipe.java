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
	public List<PinnedIngredient> ingredients = new ArrayList<>();
	public String resultItemId = "minecraft:air";
	public int resultCount = 1;
}
