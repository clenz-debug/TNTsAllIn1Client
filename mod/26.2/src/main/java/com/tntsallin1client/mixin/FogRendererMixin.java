package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "No fog", part 2/2: the render distance fog - the fade at the edge of the loaded world, which
 * {@code FogRenderer#setupFog} writes on top of whatever environment ran (see
 * {@link FogEnvironmentMixin}), so it needs its own hook. Injected right after the
 * {@code renderDistanceEnd} write - the last one before {@code setupFog} returns the {@code FogData}
 * that later goes into the fog buffer and that Sodium copies at that return (its own mixin), so
 * both see the change.
 * Removing it underwater or in lava changes nothing visible while that fog is kept, since the
 * environmental fog there is far shorter anyway.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F",
			opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
	private void tntsallin1client$removeRenderDistanceFog(Camera camera, int renderDistanceChunks, DeltaTracker deltaTracker, float darkenWorldAmount,
			ClientLevel level, CallbackInfoReturnable<FogData> cir, @Local FogData fogData) {
		ClientConfig config = ClientConfig.get();
		if (config.noFogEnabled && config.noFogDistance) {
			fogData.renderDistanceStart = Float.MAX_VALUE;
			fogData.renderDistanceEnd = Float.MAX_VALUE;
		}
	}
}
