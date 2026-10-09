package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.DarkModePack;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Light container labels while our dark mode pack is active, see {@link DarkModePack}. Every other
 * {@code drawString}/{@code drawCenteredString}/{@code drawWordWrap} variant ends in this one method (checked
 * with javap), so this is the single place all GUI text passes through. Third int = the colour.
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
	@ModifyVariable(
			method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
			at = @At("HEAD"),
			ordinal = 2,
			argsOnly = true)
	private int tntsallin1client$darkModeLabelColor(int color) {
		return DarkModePack.recolorLabel(color);
	}
}
