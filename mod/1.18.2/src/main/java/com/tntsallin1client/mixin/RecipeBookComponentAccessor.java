package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Phase 5ah (pinned recipes): {@code recipeBookPage} is a private field with no public getter -
 * needed to reach {@link RecipeBookPageAccessor}'s hovered-button state. */
@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Accessor("recipeBookPage")
	RecipeBookPage tntsallin1client$getRecipeBookPage();
}
