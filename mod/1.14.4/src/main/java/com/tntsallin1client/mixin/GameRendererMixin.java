package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer;
import com.tntsallin1client.waypoint.WaypointRenderer;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zoom: the renderer reads the field of view from the game's settings as it goes - while zooming it
 * gets ours instead. Only the world's field of view comes from the settings; the hand is drawn with
 * a fixed one and stays as it is. Also where the first-person hand is drawn, which the freecam hides,
 * and where the waypoints and the light level overlay get drawn into the finished world.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Redirect(method = "getFov", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;fov:D"))
	private double tntsallin1client$zoomFov(Options options) {
		return ZoomHandler.fov(options.fov);
	}

	/**
	 * Freecam: the hand in first person is always the player's own, wherever the camera is - it would
	 * hang in the freecam's view without belonging there, and there is nothing to do with it anyway.
	 */
	/**
	 * Waypoints and the light level overlay: the world is drawn, the view it was drawn with is still
	 * set, and the hand comes next - the game names that step "hand" for its profiler.
	 */
	@Inject(method = "render(FJ)V",
			at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
	private void tntsallin1client$renderIntoWorld(float partialTick, long finishTimeNano, CallbackInfo ci) {
		// The overlay first: it lies on the ground, the waypoints show through everything.
		SpawnOverlayRenderer.render();
		WaypointRenderer.render();
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$hideHandWhileFreecam(Camera camera, float partialTick, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}
}
