package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle filter, see {@link ParticleFilter}: the pieces flying off a block that is being mined or
 * has just been broken. Both methods build their particles themselves and hand them straight to the
 * engine, and do nothing else (bytecode-verified - the sounds come from elsewhere).
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterBrokenBlockParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.BLOCK)) {
			ci.cancel();
		}
	}

	@Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterMiningParticles(BlockPos pos, Direction side, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.BLOCK)) {
			ci.cancel();
		}
	}
}
