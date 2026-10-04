package com.tntsallin1client.fog;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;

/**
 * "No fog": removes the chosen kinds of fog. The game sets its fog up anew before every part of the
 * world it draws (`GameRenderer#renderFog`); called right after that, this overwrites what was set.
 * The branches are the game's own, in its order (bytecode-checked): blindness, inside a cloud,
 * water, lava, and the fog at the edge of the view distance otherwise. Blindness is an effect, not
 * scenery, and stays.
 */
public final class NoFog {
	/** The pass that draws the sky - its fog only shapes the horizon. */
	private static final int SKY_PASS = -1;
	/** Linear fog starts this far away: nothing that is drawn lies behind it. */
	private static final float OUT_OF_SIGHT = 1.0E6F;

	private NoFog() {
	}

	/** `inCloud` is the renderer's own flag for a camera inside a cloud. */
	public static void apply(MinecraftClient client, boolean inCloud, int pass, float tickDelta) {
		ClientConfig config = ClientConfig.get();
		Entity camera = client.getCameraEntity();
		if (!config.noFogEnabled || camera == null || client.world == null) {
			return;
		}
		if (camera instanceof LivingEntity && ((LivingEntity) camera).hasStatusEffect(StatusEffect.BLINDNESS)) {
			return;
		}
		Material material = Camera.getSubmergedBlock(client.world, camera, tickDelta).getMaterial();
		if (inCloud) {
			if (config.noFogDistance) {
				GlStateManager.fogDensity(0.0F);
			}
		} else if (material == Material.WATER) {
			if (config.noFogWater) {
				GlStateManager.fogDensity(0.0F);
			}
		} else if (material == Material.LAVA) {
			if (config.noFogLava) {
				GlStateManager.fogDensity(0.0F);
			}
		} else if (config.noFogDistance && pass != SKY_PASS) {
			GlStateManager.fogStart(OUT_OF_SIGHT);
			GlStateManager.fogEnd(2.0F * OUT_OF_SIGHT);
		}
	}
}
