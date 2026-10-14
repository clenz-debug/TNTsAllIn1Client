package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Particle filter, see {@link ParticleFilter}. {@code createParticle} is where every particle with
 * a registered type is made, and it already returns null when there is none to make (no provider,
 * particle limit reached). Its two callers (bytecode-verified): {@code ClientLevel#doAddParticle}
 * drops the result, {@code FireworkParticles.Starter#createParticle} uses it unchecked - that one is
 * stopped before it gets here, see {@link FireworkStarterMixin}.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterParticle(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed,
			double zSpeed, CallbackInfoReturnable<Particle> cir) {
		if (ParticleFilter.hides(options.getType())) {
			cir.setReturnValue(null);
		}
	}

	/** The pieces flying off a block that is being mined - built here and added without {@code createParticle}. */
	@Inject(method = "crack", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterMiningParticles(BlockPos pos, Direction side, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.BLOCK)) {
			ci.cancel();
		}
	}
}
