package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5l, extended 5aa/5ab: recolors the F3+B hitbox outline. In 1.21.4 the game still hardcodes
 * its colors inside the private static {@code renderHitbox} (the render states for hitboxes came
 * with 1.21.5) - no clean extension point, so this cancels the whole method and redraws it itself,
 * part by part the way the game does, each one independently toggleable and colorable
 * ({@link ClientConfig}'s {@code customHitboxShow*}/{@code customHitbox*Color} fields), so a chosen
 * main hitbox color can't end up matching one of them.
 *
 * <p>Only the normal call, which passes white for the main box - the game's other one (a rare
 * dev-only "compare against the local server entity" overlay, in green) is left untouched. The pose
 * is at the entity's position here, so every box is moved back by it.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	@Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
	private static void tntsallin1client$onRenderHitbox(PoseStack poseStack, VertexConsumer consumer, Entity entity, float partialTick,
			float red, float green, float blue, CallbackInfo ci) {
		ClientConfig config = ClientConfig.get();
		boolean normalCall = red == 1.0F && green == 1.0F && blue == 1.0F;
		if (!normalCall || !config.customHitboxColorEnabled) {
			return;
		}

		AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
		tntsallin1client$box(poseStack, consumer, box, config.customHitboxColor);

		if (entity instanceof EnderDragon enderDragon && config.customHitboxShowDragonParts) {
			double x = -Mth.lerp(partialTick, entity.xOld, entity.getX());
			double y = -Mth.lerp(partialTick, entity.yOld, entity.getY());
			double z = -Mth.lerp(partialTick, entity.zOld, entity.getZ());
			for (EnderDragonPart part : enderDragon.getSubEntities()) {
				poseStack.pushPose();
				poseStack.translate(x + Mth.lerp(partialTick, part.xOld, part.getX()), y + Mth.lerp(partialTick, part.yOld, part.getY()),
						z + Mth.lerp(partialTick, part.zOld, part.getZ()));
				tntsallin1client$box(poseStack, consumer, part.getBoundingBox().move(-part.getX(), -part.getY(), -part.getZ()),
						config.customHitboxDragonPartsColor);
				poseStack.popPose();
			}
		}

		if (entity instanceof LivingEntity && config.customHitboxShowEyeHeight) {
			tntsallin1client$box(poseStack, consumer, new AABB(box.minX, entity.getEyeHeight() - 0.01F, box.minZ,
					box.maxX, entity.getEyeHeight() + 0.01F, box.maxZ), config.customHitboxEyeHeightColor);
		}

		Entity vehicle = entity.getVehicle();
		if (vehicle != null && config.customHitboxShowVehicleMarker) {
			float halfWidth = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
			Vec3 mountPos = vehicle.getPassengerRidingPosition(entity).subtract(entity.position());
			tntsallin1client$box(poseStack, consumer, new AABB(mountPos.x - halfWidth, mountPos.y, mountPos.z - halfWidth,
					mountPos.x + halfWidth, mountPos.y + 0.0625, mountPos.z + halfWidth), config.customHitboxVehicleMarkerColor);
		}

		if (config.customHitboxShowViewDirection) {
			ShapeRenderer.renderVector(poseStack, consumer, new Vector3f(0.0F, entity.getEyeHeight(), 0.0F),
					entity.getViewVector(partialTick).scale(2.0), config.customHitboxViewDirectionColor | 0xFF000000);
		}
		ci.cancel();
	}

	@Unique
	private static void tntsallin1client$box(PoseStack poseStack, VertexConsumer consumer, AABB box, int color) {
		ShapeRenderer.renderLineBox(poseStack, consumer, box, ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), 1.0F);
	}
}
