package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Freecam: {@code LevelRenderer#extractVisibleEntities} has two separate rules for whether an
 * entity gets rendered (bytecode-verified): (1) the entity the camera is currently attached to is
 * hidden unless in third person - already correctly handled just by the camera now being attached
 * to {@link com.tntsallin1client.freecam.FreecamEntity} instead of the real player; and (2) an
 * entirely separate, unconditional rule specific to {@code LocalPlayer} (the only instance of
 * which is ever {@code Minecraft.player}): it is rendered *only* if it is also the camera's
 * current entity, in every camera type - normally a no-op since the camera is almost always
 * attached to the real player, but while freecam has it attached to a different entity instead,
 * this rule alone hides the real (frozen) player outright, with no camera-type workaround
 * possible. That 4th {@code Camera#entity()} call in the method (bytecode-confirmed ordinal, both
 * this and 26.1.2) only ever feeds that LocalPlayer-specific comparison - the entity being checked
 * at that point is guaranteed to already be {@code Minecraft.player} itself (it's the only
 * {@code LocalPlayer} there is), so redirecting it to just return the real player while freecam is
 * active makes the comparison trivially match and keeps them visible, without touching rule (1)
 * at all.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Redirect(method = "extractVisibleEntities", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;", ordinal = 3))
	private Entity tntsallin1client$keepRealPlayerVisibleDuringFreecam(Camera camera) {
		Minecraft client = Minecraft.getInstance();
		if (FreecamHandler.isActive() && client.player != null) {
			return client.player;
		}
		return camera.entity();
	}
}
