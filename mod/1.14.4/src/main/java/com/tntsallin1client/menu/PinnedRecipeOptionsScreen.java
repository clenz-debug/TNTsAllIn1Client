package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options of the pinned recipes: on/off, the sub-recipes, whether ingredients the player already has
 * stay listed, the three colors (counts, arrow, sub-recipe counts), the two keys (pin, open the
 * list) and the way to the list ({@link PinnedRecipeListScreen}) - in the order the newer versions
 * have them.
 */
public class PinnedRecipeOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.pinned_recipe_options.";

	public PinnedRecipeOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.pinnedRecipeEnabled, value -> config.pinnedRecipeEnabled = value);
		addToggle(KEY + "show_sub_ingredients", () -> config.pinnedRecipeShowSubIngredients, value -> config.pinnedRecipeShowSubIngredients = value);
		addToggle(KEY + "show_full_amounts", () -> config.pinnedRecipeShowFullAmounts, value -> config.pinnedRecipeShowFullAmounts = value);
		addHeading(KEY + "count_color_label");
		addColor(() -> config.pinnedRecipeCountColor, argb -> config.pinnedRecipeCountColor = argb);
		addHeading(KEY + "arrow_color_label");
		addColor(() -> config.pinnedRecipeArrowColor, argb -> config.pinnedRecipeArrowColor = argb);
		addHeading(KEY + "sub_count_color_label");
		addColor(() -> config.pinnedRecipeSubIngredientCountColor, argb -> config.pinnedRecipeSubIngredientCountColor = argb);
		addKeyBinding(KEY + "key", ModKeyBindings.PIN_RECIPE);
		addKeyBinding(KEY + "open_key", ModKeyBindings.OPEN_PINNED_RECIPES);
		addLink(KEY + "manage_button", () -> new PinnedRecipeListScreen(this));
		setHint(KEY + "hint");
		setResettable(ConfigReset.Feature.PINNED_RECIPE);
	}
}
