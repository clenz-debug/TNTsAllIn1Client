package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Block outline color: recolors the wireframe around the block the player looks at. The game sets
 * the color once before drawing it (black, see-through); only that color is swapped - the outline
 * stays as see-through as it always was.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	@Redirect(method = "drawBlockOutline(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;IF)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFFF)V"))
	private void tnt$blockOutlineColor(float red, float green, float blue, float alpha) {
		ClientConfig config = ClientConfig.get();
		if (config.customBlockOutlineColorEnabled) {
			int color = config.customBlockOutlineColor;
			GlStateManager.color(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
		} else {
			GlStateManager.color(red, green, blue, alpha);
		}
	}
}
