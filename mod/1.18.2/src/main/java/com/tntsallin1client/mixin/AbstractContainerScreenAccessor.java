package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code leftPos}/{@code topPos}/{@code imageWidth} are protected fields with no public getter -
 * needed by {@link com.tntsallin1client.inventory.QuickSortUi} to read the panel's real,
 * already-computed position (which shifts when the recipe book is open) instead of re-guessing
 * a centered position that goes wrong whenever vanilla's own layout doesn't match that guess.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int tntsallin1client$getLeftPos();

	@Accessor("topPos")
	int tntsallin1client$getTopPos();

	@Accessor("imageWidth")
	int tntsallin1client$getImageWidth();
}
