package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5l, extended 5aa/5ab: recolors the F3+B hitbox outline. Vanilla
 * hardcodes its colors inside the private static {@code renderHitbox} - no
 * clean extension point, so this cancels the whole method and redraws it
 * itself, part by part the way the game does (bytecode-read), each one
 * independently toggleable and colorable ({@link ClientConfig}'s
 * {@code customHitboxShow*}/{@code customHitbox*Color} fields), so a chosen
 * main hitbox color can't end up matching one of them.
 *
 * <p>The pose is at the entity's position here, so every box is moved back by
 * it.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	@Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
	private static void tntsallin1client$onRenderHitbox(PoseStack poseStack, VertexConsumer consumer, Entity entity, float partialTick,
			CallbackInfo ci) {
		ClientConfig config = ClientConfig.get();
		if (!config.customHitboxColorEnabled) {
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
			Vec3 view = entity.getViewVector(partialTick).scale(2.0);
			PoseStack.Pose pose = poseStack.last();
			float eyeHeight = entity.getEyeHeight();
			int color = config.customHitboxViewDirectionColor;
			consumer.vertex(pose, 0.0F, eyeHeight, 0.0F).color(color).normal(pose, (float) view.x, (float) view.y, (float) view.z).endVertex();
			consumer.vertex(pose, (float) view.x, (float) (eyeHeight + view.y), (float) view.z).color(color)
					.normal(pose, (float) view.x, (float) view.y, (float) view.z).endVertex();
		}
		ci.cancel();
	}

	@Unique
	private static void tntsallin1client$box(PoseStack poseStack, VertexConsumer consumer, AABB box, int color) {
		LevelRenderer.renderLineBox(poseStack, consumer, box, FastColor.ARGB32.red(color) / 255.0F, FastColor.ARGB32.green(color) / 255.0F,
				FastColor.ARGB32.blue(color) / 255.0F, 1.0F);
	}
}
