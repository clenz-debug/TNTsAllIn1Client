package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5l, extended Phase 5aa: recolors the F3+B hitbox outline. Vanilla
 * hardcodes it to white (int {@code -1}) inside the private {@code
 * showHitboxes(Entity, float, boolean)} - no clean extension point like 5d's
 * DebugScreenEntries API this time, so this cancels the whole method and
 * redraws it itself with our color, when {@code bl} is false (the normal
 * path; {@code bl} true is a rare dev-only "compare against the local server
 * entity" overlay, left untouched).
 *
 * <p>5l originally only redrew the main box + position point and dropped the
 * secondary vanilla indicators (vehicle mount marker, eye-height line,
 * view-direction arrow, ender dragon sub-parts) as a simplification. 5aa
 * restores all of them, 1:1 copied from vanilla's own method body (including
 * its hardcoded colors - only the main box and position point use our custom
 * color, the rest stays visually distinct exactly like in vanilla F3+B).
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
	@Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onShowHitboxes(Entity entity, float partialTick, boolean bl, CallbackInfo ci) {
		if (bl || !ClientConfig.get().customHitboxColorEnabled) {
			return;
		}

		int color = ClientConfig.get().customHitboxColor;
		Vec3 vec3 = entity.position();
		Vec3 renderPos = entity.getPosition(partialTick);
		Vec3 delta = renderPos.subtract(vec3);
		Gizmos.cuboid(entity.getBoundingBox().move(delta), GizmoStyle.stroke(color));
		Gizmos.point(renderPos, color, 2.0F);

		Entity vehicle = entity.getVehicle();
		if (vehicle != null) {
			float halfWidth = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
			Vec3 mountPos = vehicle.getPassengerRidingPosition(entity).add(delta);
			Gizmos.cuboid(new AABB(mountPos.x - halfWidth, mountPos.y, mountPos.z - halfWidth,
					mountPos.x + halfWidth, mountPos.y + 0.0625, mountPos.z + halfWidth), GizmoStyle.stroke(-256));
		}

		if (entity instanceof LivingEntity) {
			AABB box = entity.getBoundingBox().move(delta);
			Gizmos.cuboid(new AABB(box.minX, box.minY + entity.getEyeHeight() - 0.01F, box.minZ,
					box.maxX, box.minY + entity.getEyeHeight() + 0.01F, box.maxZ), GizmoStyle.stroke(-65536));
		}

		if (entity instanceof EnderDragon enderDragon) {
			for (EnderDragonPart part : enderDragon.getSubEntities()) {
				Vec3 partDelta = part.getPosition(partialTick).subtract(part.position());
				Gizmos.cuboid(part.getBoundingBox().move(partDelta), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F)));
			}
		}

		Vec3 eyePos = renderPos.add(0.0, entity.getEyeHeight(), 0.0);
		Vec3 viewVector = entity.getViewVector(partialTick);
		Gizmos.arrow(eyePos, eyePos.add(viewVector.scale(2.0)), -16776961);

		ci.cancel();
	}
}
