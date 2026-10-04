package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.fog.NoFog;
import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer;
import com.tntsallin1client.tour.TourOverlay;
import com.tntsallin1client.waypoint.WaypointRenderer;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The renderer reads the field of view, the mouse sensitivity and the brightness from the game's
 * settings as it goes - where one of our features wants another value, it gets ours instead. It is
 * also where the mouse turns the player and where the first-person hand is drawn, both of which the
 * freecam takes over, where the waypoints and the light level overlay get drawn into the finished
 * world, where the fog is set up that "No fog" takes away again, and where the screen on display is
 * drawn - with the in-game tour's explanation on top of it.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	private static final float FULLBRIGHT_GAMMA = 100.0F;

	@Shadow
	private MinecraftClient client;

	/** Set while the camera is inside a cloud. */
	@Shadow
	private boolean thickFog;

	/** The in-game tour: its explanation goes on top of whatever screen the game has just drawn. */
	@Inject(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(IIF)V", shift = At.Shift.AFTER))
	private void tnt$drawTourOverScreen(float tickDelta, long limitTime, CallbackInfo ci) {
		TourOverlay.renderScreen(this.client.currentScreen);
	}

	/** No fog: the game has just set up the fog for the next part of the world it draws. */
	@Inject(method = "renderFog(IF)V", at = @At("RETURN"))
	private void tnt$removeFog(int pass, float tickDelta, CallbackInfo ci) {
		NoFog.apply(this.client, this.thickFog, pass, tickDelta);
	}

	/** Only the world's field of view comes from the settings; the hand is drawn with a fixed one and stays as it is. */
	@Redirect(method = "getFov(FZ)F", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fov:F"))
	private float tnt$zoomFov(GameOptions options) {
		return ZoomHandler.fov(options.fov);
	}

	@Redirect(method = "render(FJ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;sensitivity:F"))
	private float tnt$zoomSensitivity(GameOptions options) {
		return ZoomHandler.sensitivity(options.sensitivity);
	}

	/**
	 * Fullbright: the light table the world is drawn with is recalculated every tick from the
	 * brightness setting. A value far past the slider's range makes every entry of it fully bright -
	 * handed in here, the setting itself (and `options.txt`) stays what the player chose.
	 */
	@Redirect(method = "updateLightmap(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;gamma:F"))
	private float tnt$fullbrightGamma(GameOptions options) {
		return ClientConfig.get().fullbrightEnabled ? FULLBRIGHT_GAMMA : options.gamma;
	}

	/**
	 * Freecam: this is where the game turns the player by what the mouse moved since the last frame
	 * (both of its calls, with and without the smooth camera). While freecam is active the camera is
	 * turned instead and the player keeps looking where it looked.
	 */
	@Redirect(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;increaseTransforms(FF)V"))
	private void tnt$turnFreecamInsteadOfPlayer(ClientPlayerEntity player, float yawChange, float pitchChange) {
		if (FreecamHandler.isActive()) {
			FreecamHandler.turn(yawChange, pitchChange);
		} else {
			player.increaseTransforms(yawChange, pitchChange);
		}
	}

	/**
	 * Freecam: the hand in first person is always the player's own, wherever the camera is - it would
	 * hang in the freecam's view without belonging there, and there is nothing to do with it anyway.
	 */
	@Inject(method = "renderHand(FI)V", at = @At("HEAD"), cancellable = true)
	private void tnt$hideHandWhileFreecam(float tickDelta, int anaglyphPass, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	/**
	 * Waypoints and the light level overlay: the world is drawn, the view it was drawn with is still set, and the hand comes next -
	 * the game names that step "hand" for its profiler.
	 */
	@Inject(method = "renderWorld(IFJ)V",
			at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
	private void tnt$renderIntoWorld(int anaglyphPass, float tickDelta, long limitTime, CallbackInfo ci) {
		// The overlay first: it lies on the ground, the waypoints show through everything.
		SpawnOverlayRenderer.render(tickDelta);
		WaypointRenderer.render(tickDelta);
	}
}
