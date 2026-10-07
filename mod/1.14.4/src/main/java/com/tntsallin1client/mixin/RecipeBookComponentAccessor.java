package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Pinned recipes: the recipe book's page of recipe buttons, which the game hands out to no one. */
@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Accessor("recipeBookPage")
	RecipeBookPage tntsallin1client$getRecipeBookPage();
}
