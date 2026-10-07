package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hitbox color: recolors what F3+B draws around an entity. The game draws four things there, each
 * in a fixed color (bytecode-checked, in this order) - the box itself (white), the boxes of an ender
 * dragon's parts (green), a flat box at eye height for living entities (red) and a line in the
 * direction it looks (blue). With the feature on, the box gets the player's color and the other
 * three are indicators that are off until switched on, each with a color of its own - as in the
 * newer versions of the mod (whose game versions have one more indicator this one lacks).
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	private static final String DRAW_BOX = "Lnet/minecraft/client/renderer/LevelRenderer;renderLineBox(DDDDDDFFFF)V";

	/** The first box drawn is the hitbox itself. */
	@Redirect(method = "renderHitbox", at = @At(value = "INVOKE", target = DRAW_BOX, ordinal = 0))
	private void tntsallin1client$hitboxColor(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			float red, float green, float blue, float alpha) {
		ClientConfig config = ClientConfig.get();
		if (config.customHitboxColorEnabled) {
			drawBox(minX, minY, minZ, maxX, maxY, maxZ, config.customHitboxColor);
		} else {
			LevelRenderer.renderLineBox(minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		}
	}

	/** The second one, in a loop, is each part of an ender dragon. */
	@Redirect(method = "renderHitbox", at = @At(value = "INVOKE", target = DRAW_BOX, ordinal = 1))
	private void tntsallin1client$dragonPartsIndicator(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			float red, float green, float blue, float alpha) {
		ClientConfig config = ClientConfig.get();
		if (!config.customHitboxColorEnabled) {
			LevelRenderer.renderLineBox(minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		} else if (config.customHitboxShowDragonParts) {
			drawBox(minX, minY, minZ, maxX, maxY, maxZ, config.customHitboxDragonPartsColor);
		}
	}

	/** The third one marks the eye height. */
	@Redirect(method = "renderHitbox", at = @At(value = "INVOKE", target = DRAW_BOX, ordinal = 2))
	private void tntsallin1client$eyeHeightIndicator(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			float red, float green, float blue, float alpha) {
		ClientConfig config = ClientConfig.get();
		if (!config.customHitboxColorEnabled) {
			LevelRenderer.renderLineBox(minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		} else if (config.customHitboxShowEyeHeight) {
			drawBox(minX, minY, minZ, maxX, maxY, maxZ, config.customHitboxEyeHeightColor);
		}
	}

	/** Both ends of the view direction line get their color here. */
	@Redirect(method = "renderHitbox", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;color(IIII)Lcom/mojang/blaze3d/vertex/BufferBuilder;"))
	private BufferBuilder tntsallin1client$viewDirectionColor(BufferBuilder buffer, int red, int green, int blue, int alpha) {
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
	@Redirect(method = "renderHitbox", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 tntsallin1client$viewDirectionIndicator(Entity entity, float partialTick) {
		ClientConfig config = ClientConfig.get();
		if (config.customHitboxColorEnabled && !config.customHitboxShowViewDirection) {
			return Vec3.ZERO;
		}
		return entity.getViewVector(partialTick);
	}

	private static void drawBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
		LevelRenderer.renderLineBox(minX, minY, minZ, maxX, maxY, maxZ,
				((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
	}
}
