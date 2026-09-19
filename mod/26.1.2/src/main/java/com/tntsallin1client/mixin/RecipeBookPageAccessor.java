package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Phase 5ah (pinned recipes): {@code hoveredButton} is set every frame in {@code render()} from
 * whichever button is currently hovered/focused - null when nothing is. Only way to know which
 * recipe the mouse is over when the pin key is pressed, vanilla has no public accessor for it. */
@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
	@Accessor("hoveredButton")
	@Nullable RecipeButton tntsallin1client$getHoveredButton();
}
