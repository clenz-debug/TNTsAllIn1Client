package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Phase 5ah (pinned recipes): {@code recipeBookComponent} is a private field on the abstract
 * base every recipe-book-having screen (crafting table, survival/creative inventory) extends -
 * no public getter exists. Targeting the abstract class here still applies to every subclass
 * instance, since the generated accessor method is inherited like any other method.
 */
@Mixin(AbstractRecipeBookScreen.class)
public interface AbstractRecipeBookScreenAccessor {
	@Accessor("recipeBookComponent")
	RecipeBookComponent<?> tntsallin1client$getRecipeBookComponent();
}
