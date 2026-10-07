package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Block outline color: recolors the wireframe around the block the player looks at. The game hands
 * the color (black, see-through) to the method that draws the shape; only red, green and blue are
 * swapped - the outline stays as see-through as it always was.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Redirect(method = "renderHitOutline",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderShape(Lnet/minecraft/world/phys/shapes/VoxelShape;DDDFFFF)V"))
	private void tntsallin1client$blockOutlineColor(VoxelShape shape, double x, double y, double z, float red, float green, float blue, float alpha) {
		ClientConfig config = ClientConfig.get();
		if (config.customBlockOutlineColorEnabled) {
			int color = config.customBlockOutlineColor;
			LevelRenderer.renderShape(shape, x, y, z, ((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
		} else {
			LevelRenderer.renderShape(shape, x, y, z, red, green, blue, alpha);
		}
	}
}
