package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The renderer reads the field of view, the mouse sensitivity and the brightness from the game's
 * settings as it goes - where one of our features wants another value, it gets ours instead.
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
}
