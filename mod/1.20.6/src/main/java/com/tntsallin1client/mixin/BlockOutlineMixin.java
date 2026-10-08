package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Phase 5q: recolors the block-targeting wireframe outline. The private
 * {@code LevelRenderer#renderHitOutline} hands the shape to {@code renderShape}
 * with black at 40 % alpha (bytecode-read) - that one call is wrapped and gets
 * our red, green and blue. The alpha is deliberately kept: swapping in a fully
 * opaque color here would make the outline solid instead of the customary
 * translucent look vanilla always had.
 */
@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {
	@WrapOperation(method = "renderHitOutline", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderShape(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/shapes/VoxelShape;DDDFFFF)V"))
	private void tntsallin1client$recolorOutline(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double x, double y, double z,
			float red, float green, float blue, float alpha, Operation<Void> original) {
		ClientConfig config = ClientConfig.get();
		if (!config.customBlockOutlineColorEnabled) {
			original.call(poseStack, consumer, shape, x, y, z, red, green, blue, alpha);
			return;
		}
		int color = config.customBlockOutlineColor;
		original.call(poseStack, consumer, shape, x, y, z, FastColor.ARGB32.red(color) / 255.0F, FastColor.ARGB32.green(color) / 255.0F,
				FastColor.ARGB32.blue(color) / 255.0F, alpha);
	}
}
