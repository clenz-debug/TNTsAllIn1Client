package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hitbox color: recolors what F3+B draws around an entity. The game draws three things there, each
 * in a fixed color - the box itself (white), a flat box at eye height for living entities (red) and
 * a line in the direction it looks (blue). With the feature on, the box gets the player's color and
 * the other two are indicators that are off until switched on, each with a color of its own - as in
 * the Fabric versions of the mod (whose game versions have two more indicators this one lacks).
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	private static final String RENDER_HITBOX = "renderHitbox(Lnet/minecraft/entity/Entity;DDDFF)V";
	private static final String DRAW_BOX = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/util/math/Box;IIII)V";

	/** The first box drawn is the hitbox itself. */
	@Redirect(method = RENDER_HITBOX, at = @At(value = "INVOKE", target = DRAW_BOX, ordinal = 0))
	private void tnt$hitboxColor(Box box, int red, int green, int blue, int alpha) {
		ClientConfig config = ClientConfig.get();
		if (config.customHitboxColorEnabled) {
			drawBox(box, config.customHitboxColor);
		} else {
			WorldRenderer.drawBox(box, red, green, blue, alpha);
		}
	}

	/** The second one marks the eye height. */
	@Redirect(method = RENDER_HITBOX, at = @At(value = "INVOKE", target = DRAW_BOX, ordinal = 1))
	private void tnt$eyeHeightIndicator(Box box, int red, int green, int blue, int alpha) {
		ClientConfig config = ClientConfig.get();
		if (!config.customHitboxColorEnabled) {
			WorldRenderer.drawBox(box, red, green, blue, alpha);
		} else if (config.customHitboxShowEyeHeight) {
			drawBox(box, config.customHitboxEyeHeightColor);
		}
	}

	/** Both ends of the view direction line get their color here. */
	@Redirect(method = RENDER_HITBOX, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;color(IIII)Lnet/minecraft/client/render/BufferBuilder;"))
	private BufferBuilder tnt$viewDirectionColor(BufferBuilder buffer, int red, int green, int blue, int alpha) {
		ClientConfig config = ClientConfig.get();
		if (!config.customHitboxColorEnabled) {
			return buffer.color(red, green, blue, alpha);
		}
		int color = config.customHitboxViewDirectionColor;
		return buffer.color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);
	}

	/**
	 * The view direction line runs from the eyes two blocks along this vector. The line is always
	 * drawn once its drawing has begun, so it is hidden by giving it no length.
	 */
	@Redirect(method = RENDER_HITBOX, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVector(F)Lnet/minecraft/util/math/Vec3d;"))
	private Vec3d tnt$viewDirectionIndicator(Entity entity, float tickDelta) {
		ClientConfig config = ClientConfig.get();
		if (config.customHitboxColorEnabled && !config.customHitboxShowViewDirection) {
			return new Vec3d(0.0, 0.0, 0.0);
		}
		return entity.getRotationVector(tickDelta);
	}

	private static void drawBox(Box box, int color) {
		WorldRenderer.drawBox(box, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);
	}
}
