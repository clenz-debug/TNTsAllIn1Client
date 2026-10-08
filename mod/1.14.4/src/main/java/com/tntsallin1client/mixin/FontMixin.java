package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.DarkMode;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Dark mode: the game draws the labels of its container screens ("Inventory", "Crafting") in a
 * fixed dark grey through this method - while the dark mode pack is active they get the color the
 * pack names instead (see {@link DarkMode}). Every other color passes through as it is.
 */
@Mixin(Font.class)
public abstract class FontMixin {
	@ModifyVariable(method = "draw(Ljava/lang/String;FFI)I", at = @At("HEAD"), argsOnly = true)
	private int tntsallin1client$darkModeLabelColor(int color) {
		return DarkMode.textColor(color);
	}
}
