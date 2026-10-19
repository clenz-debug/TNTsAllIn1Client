package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.DarkModePack;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Light container labels while our dark mode pack is active, see {@link DarkModePack}. 1.18.2 has no
 * {@code GuiGraphics}: every {@code Font#draw}/{@code drawShadow} variant a screen uses ends in one of
 * these two methods (checked with javap). Third int after the coordinates = the colour.
 */
@Mixin(Font.class)
public class FontMixin {
	@ModifyVariable(
			method = {
					"drawInternal(Ljava/lang/String;FFILcom/mojang/math/Matrix4f;ZZ)I",
					"drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFILcom/mojang/math/Matrix4f;Z)I"},
			at = @At("HEAD"),
			ordinal = 0,
			argsOnly = true)
	private int tntsallin1client$darkModeLabelColor(int color) {
		return DarkModePack.recolorLabel(color);
	}
}
