package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.particle.ParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle filter, see {@link ParticleFilter}: the sparks of a firework explosion, which the
 * explosion builds itself and adds directly. The explosion keeps running - it also plays the sounds.
 */
@Mixin(FireworksSparkParticle.FireworkParticle.class)
public abstract class FireworkParticleMixin {
	@Inject(method = "addExplosionParticle(DDDDDD[I[IZZ)V", at = @At("HEAD"), cancellable = true)
	private void tnt$filterSpark(double x, double y, double z, double velocityX, double velocityY, double velocityZ, int[] colors,
			int[] fadeColors, boolean trail, boolean flicker, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleType.FIREWORK_SPARK)) {
			ci.cancel();
		}
	}
}
