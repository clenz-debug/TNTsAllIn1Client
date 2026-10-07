package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Pinned recipes: the recipe button under the cursor - set every frame while the page is drawn, null
 * when there is none. The only way to know which recipe the mouse is over when the pin key is pressed.
 */
@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
	@Accessor("hoveredButton")
	RecipeButton tntsallin1client$getHoveredButton();
}
