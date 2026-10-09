package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.feature.HitboxFeatureRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The view-direction arrow of the F3+B hitboxes: its own color, or none at all
 * ({@code customHitboxShowViewDirection}/{@code customHitboxViewDirectionColor}). The boxes
 * themselves are recolored in {@link EntityRendererHitboxMixin}.
 */
@Mixin(HitboxFeatureRenderer.class)
public class HitboxFeatureRendererMixin {
	@Redirect(method = "renderHitboxesAndViewVector", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ShapeRenderer;renderVector(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Vector3f;Lnet/minecraft/world/phys/Vec3;I)V"))
	private static void tntsallin1client$viewDirection(PoseStack poseStack, VertexConsumer vertexConsumer, Vector3f start, Vec3 vector, int color) {
		ClientConfig config = ClientConfig.get();
		if (!config.customHitboxColorEnabled) {
			ShapeRenderer.renderVector(poseStack, vertexConsumer, start, vector, color);
		} else if (config.customHitboxShowViewDirection) {
			ShapeRenderer.renderVector(poseStack, vertexConsumer, start, vector, config.customHitboxViewDirectionColor | 0xFF000000);
		}
	}
}
