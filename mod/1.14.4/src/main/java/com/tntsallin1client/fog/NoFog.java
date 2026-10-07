package com.tntsallin1client.fog;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;

/**
 * "No fog": removes the chosen kinds of fog. The game sets its fog up anew before every part of the
 * world it draws (`FogRenderer#setupFog`); called right after that, this overwrites what was set.
 * The branches are the game's own, in its order (bytecode-checked): blindness, water, lava, and the
 * fog at the edge of the view distance otherwise - which includes the Nether's and a boss's.
 * Blindness is an effect, not scenery, and stays.
 */
public final class NoFog {
	/** The pass that draws the sky - its fog only shapes the horizon. */
	private static final int SKY_PASS = -1;
	/** Linear fog starts this far away: nothing that is drawn lies behind it. */
	private static final float OUT_OF_SIGHT = 1.0E6F;

	private NoFog() {
	}

	public static void apply(Camera camera, int pass) {
		ClientConfig config = ClientConfig.get();
		if (!config.noFogEnabled) {
			return;
		}
		Entity entity = camera.getEntity();
		if (entity instanceof LivingEntity && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS)) {
			return;
		}
		FluidState fluid = camera.getFluidInCamera();
		if (fluid.is(FluidTags.WATER)) {
			if (config.noFogWater) {
				GlStateManager.fogDensity(0.0F);
			}
		} else if (fluid.is(FluidTags.LAVA)) {
			if (config.noFogLava) {
				GlStateManager.fogDensity(0.0F);
			}
		} else if (config.noFogDistance && pass != SKY_PASS) {
			GlStateManager.fogStart(OUT_OF_SIGHT);
			GlStateManager.fogEnd(2.0F * OUT_OF_SIGHT);
		}
	}
}
