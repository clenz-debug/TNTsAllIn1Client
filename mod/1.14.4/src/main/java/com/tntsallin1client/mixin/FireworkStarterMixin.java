package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle filter, see {@link ParticleFilter}: a firework explosion. It asks the particle engine for
 * each spark and for its flash and sets them up without checking for null (bytecode-checked), so a
 * hidden one must not simply be missing. The explosion itself keeps running - it also plays the sounds.
 */
@Mixin(FireworkParticles.Starter.class)
public abstract class FireworkStarterMixin {
	/** The sparks: not asked for at all. */
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterSpark(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int[] colors,
			int[] fadeColors, boolean trail, boolean flicker, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.FIREWORK)) {
			ci.cancel();
		}
	}

	/** The flash - the one particle `tick` asks for: its color is set right after, on a stand-in if it is hidden. */
	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"))
	private Particle tntsallin1client$filterFlash(ParticleEngine engine, ParticleOptions options, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed) {
		if (ParticleFilter.hides(options.getType())) {
			return ParticleFilter.placeholder(Minecraft.getInstance().level);
		}
		return engine.createParticle(options, x, y, z, xSpeed, ySpeed, zSpeed);
	}
}
