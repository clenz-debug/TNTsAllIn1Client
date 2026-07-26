package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5l: recolors the F3+B hitbox outline. Vanilla hardcodes it to white
 * (int {@code -1}) inside the private {@code showHitboxes(Entity, float,
 * boolean)} - no clean extension point like 5d's DebugScreenEntries API this
 * time, so this cancels the whole method and redraws just the main box +
 * position point with our color, when {@code bl} is false (the normal path;
 * {@code bl} true is a rare dev-only "compare against the local server
 * entity" overlay, left untouched). Deliberately drops the secondary vanilla
 * indicators this method also draws (vehicle mount marker, eye-height line,
 * view-direction arrow, ender dragon sub-parts) rather than reimplementing
 * all of them just to recolor one box - a reasonable simplification for a
 * "moderat" feature, and lower-risk than an ordinal-based redirect trying to
 * target only the first of several identical Gizmos.cuboid(...) calls in the
 * same method.
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
		ci.cancel();
	}
}
