package com.tntsallin1client.particle;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.level.Level;

/** See {@link ParticleFilter#placeholder}. Never handed to the particle engine, so it is neither ticked nor drawn. */
final class PlaceholderParticle extends Particle {
	PlaceholderParticle(Level level) {
		super(level, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public void render(BufferBuilder buffer, Camera camera, float partialTick, float rotationX, float rotationZ, float rotationYZ,
			float rotationXY, float rotationXZ) {
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.NO_RENDER;
	}
}
