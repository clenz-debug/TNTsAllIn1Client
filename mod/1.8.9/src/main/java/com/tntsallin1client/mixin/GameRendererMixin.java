package com.tntsallin1client.mixin;

import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Zoom: the renderer reads the field of view and the mouse sensitivity from the game's settings
 * every frame - while zooming it gets ours instead.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** Only the world's field of view comes from the settings; the hand is drawn with a fixed one and stays as it is. */
	@Redirect(method = "getFov(FZ)F", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fov:F"))
	private float tnt$zoomFov(GameOptions options) {
		return ZoomHandler.fov(options.fov);
	}

	@Redirect(method = "render(FJ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;sensitivity:F"))
	private float tnt$zoomSensitivity(GameOptions options) {
		return ZoomHandler.sensitivity(options.sensitivity);
	}
}
