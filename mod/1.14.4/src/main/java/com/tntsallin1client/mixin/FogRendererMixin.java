package com.tntsallin1client.mixin;

import com.tntsallin1client.fog.NoFog;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No fog: the game has just set up the fog for the next part of the world it draws - see {@link NoFog}. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void tntsallin1client$removeFog(Camera camera, int pass, CallbackInfo ci) {
		NoFog.apply(camera, pass);
	}
}
