package com.tntsallin1client.mixin;

import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle filter, see {@link ParticleFilter}: the pieces flying off a block that is being mined or
 * has just been broken - the latter here, the former in {@link ParticleEngineMixin} ({@code crack}),
 * which the level renderer calls directly in 1.21.8.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$filterBrokenBlockParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (ParticleFilter.hides(ParticleTypes.BLOCK)) {
			ci.cancel();
		}
	}
}
