package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle filter, see {@link ParticleFilter}: the sparks of a firework explosion. The starter makes
 * each one through {@code ParticleEngine#createParticle} and sets it up without checking for null,
 * so a hidden spark has to be stopped here, before it is asked for. The starter itself keeps
 * running - it also plays the explosion's sounds.
 */
@Mixin(FireworkParticles.Starter.class)
public class FireworkStarterMixin {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterSpark(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IntList colors,
			IntList fadeColors, boolean trail, boolean twinkle, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.FIREWORK)) {
			ci.cancel();
		}
	}
}
