package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particle filter, see {@link ParticleFilter}. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
	/**
	 * Where every particle made by its type is created. The method already returns null when there
	 * is nothing to make, and its one caller (`WorldRenderer`'s own particle method) hands that on to
	 * callers that drop the result or check it.
	 */
	@Inject(method = "addParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
	private void tnt$filterParticle(int typeId, double x, double y, double z, double velocityX, double velocityY, double velocityZ,
			int[] arguments, CallbackInfoReturnable<Particle> cir) {
		if (ParticleFilter.hides(typeId)) {
			cir.setReturnValue(null);
		}
	}

	/** The pieces of a block that has just been broken - built here and added directly. */
	@Inject(method = "addBlockBreakParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("HEAD"), cancellable = true)
	private void tnt$filterBrokenBlockParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleType.BLOCK_CRACK)) {
			ci.cancel();
		}
	}

	/** The pieces flying off a block while it is being mined. */
	@Inject(method = "addBlockBreakingParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)V", at = @At("HEAD"), cancellable = true)
	private void tnt$filterMiningParticles(BlockPos pos, Direction side, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleType.BLOCK_CRACK)) {
			ci.cancel();
		}
	}
}
