package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Phase 5q: recolors the block-targeting wireframe outline. {@code LevelRenderer#submitHitOutline}
 * takes the outline's color as its only int argument ({@code submitBlockOutline} passes vanilla's
 * black, checked with javap), so swapping that argument is all it takes. The alpha byte of the
 * incoming color is deliberately kept (translucent normally, opaque in the accessibility "high
 * contrast" mode) and only the RGB is replaced - a fully opaque color here would make the outline
 * solid instead of the customary translucent look vanilla always had.
 */
@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {
	@ModifyVariable(method = "submitHitOutline", at = @At("HEAD"), argsOnly = true)
	private int tntsallin1client$recolorHitOutline(int color) {
		ClientConfig config = ClientConfig.get();
		if (!config.customBlockOutlineColorEnabled) {
			return color;
		}
		return (color & 0xFF000000) | (config.customBlockOutlineColor & 0x00FFFFFF);
	}
}
