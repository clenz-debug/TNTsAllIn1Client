package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fullbright: the light table the world is drawn with is recalculated every tick from the
 * brightness setting. A value far past the slider's range makes every entry of it fully bright -
 * handed in here, the setting itself (and `options.txt`) stays what the player chose.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	private static final double FULLBRIGHT_GAMMA = 100.0D;

	@Redirect(method = "updateLightTexture", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;gamma:D"))
	private double tntsallin1client$fullbrightGamma(Options options) {
		return ClientConfig.get().fullbrightEnabled ? FULLBRIGHT_GAMMA : options.gamma;
	}
}
