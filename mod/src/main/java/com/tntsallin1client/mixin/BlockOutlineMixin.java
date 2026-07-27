package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5q: recolors the block-targeting wireframe outline. Same situation as
 * 5l's hitbox mixin - private method ({@code LevelRenderer#renderHitOutline}),
 * no clean extension point, so this cancels it and redraws with
 * {@link ShapeRenderer#renderShape} (the exact same call vanilla itself makes
 * in the non-debug branch) using our own color. Unlike 5l, the alpha byte of
 * the incoming {@code i} param is deliberately kept (translucent normally,
 * opaque in the accessibility "high contrast" mode) and only the RGB is
 * replaced - swapping in a fully opaque color here would make the outline
 * solid instead of the customary translucent look vanilla always had.
 */
@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {
	@Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f,
			BlockOutlineRenderState blockOutlineRenderState, int i, float g, CallbackInfo ci) {
		if (!ClientConfig.get().customBlockOutlineColorEnabled) {
			return;
		}

		int color = (i & 0xFF000000) | (ClientConfig.get().customBlockOutlineColor & 0x00FFFFFF);
		BlockPos blockPos = blockOutlineRenderState.pos();
		ShapeRenderer.renderShape(poseStack, vertexConsumer, blockOutlineRenderState.shape(),
				blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - f, color, g);
		ci.cancel();
	}
}
