package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The renderer reads the field of view, the mouse sensitivity and the brightness from the game's
 * settings as it goes - where one of our features wants another value, it gets ours instead. It is
 * also where the mouse turns the player and where the first-person hand is drawn, both of which the
 * freecam takes over.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	private static final float FULLBRIGHT_GAMMA = 100.0F;

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
}
