package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particle filter, see {@link ParticleFilter}. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	/**
	 * Where every particle with a registered type is made; it already returns null when there is none
	 * to make. Its callers (bytecode-checked): `LevelRenderer#addParticleInternal` hands the result on
	 * to callers that drop or check it; the firework explosion uses it unchecked and is dealt with in
	 * {@link FireworkStarterMixin}.
	 */
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterParticle(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed,
			double zSpeed, CallbackInfoReturnable<Particle> cir) {
		if (ParticleFilter.hides(options.getType())) {
			cir.setReturnValue(null);
		}
	}

	/** The pieces of a block that has just been broken - built here and added directly. */
	@Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterBrokenBlockParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.BLOCK)) {
			ci.cancel();
		}
	}

	/** The pieces flying off a block while it is being mined. */
	@Inject(method = "crack", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterMiningParticles(BlockPos pos, Direction side, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.BLOCK)) {
			ci.cancel();
		}
	}
}
