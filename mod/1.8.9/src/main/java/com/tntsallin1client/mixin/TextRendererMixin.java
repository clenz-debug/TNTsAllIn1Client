package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.DarkMode;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Dark mode: the labels of the game's screens are drawn in a fixed dark grey, which this swaps for
 * a light one while the dark mode pack is active (see {@link DarkMode}). All text the game draws
 * ends in one of these two methods - a single line, or a text broken into lines.
 */
@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
	@ModifyVariable(method = "draw(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int tnt$darkModeLabel(int color) {
		return DarkMode.textColor(color);
	}

	/** The fourth whole number of x, y, width, color. */
	@ModifyVariable(method = "drawTrimmed(Ljava/lang/String;IIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 3)
	private int tnt$darkModeWrappedLabel(int color) {
		return DarkMode.textColor(color);
	}
}
