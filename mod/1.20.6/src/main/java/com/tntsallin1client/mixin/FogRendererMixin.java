package com.tntsallin1client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No fog": pushes the fog of the chosen fog types out to infinity. {@code FogRenderer#setupFog}
 * picks exactly one kind of fog in this order (bytecode-read): lava, powder snow, blindness or
 * darkness, water, and otherwise the normal air fog - which in 1.20.6 is the fade at the edge of the
 * loaded world, rain, the Nether/End fog and the boss fog in one. At its end it hands start and end
 * to the render system; overriding them there removes exactly the fog type that ran. Blindness and
 * darkness are left alone, so those effects keep working.
 *
 * <p>{@link Float#MAX_VALUE}: the fog shaders (vanilla's and Sodium's) return zero fog for any
 * distance at or below the start, and Sodium reads the same two values, so it follows along without
 * a mixin of its own. The sky pass keeps its fog for the air (it only shapes the horizon); in a
 * fluid, where the game ties it to the fluid's tiny distance, it gets what the air's sky pass uses.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void tntsallin1client$removeFog(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog,
			float partialTick, CallbackInfo ci) {
		ClientConfig config = ClientConfig.get();
		if (!config.noFogEnabled) {
			return;
		}
		FogType fluid = camera.getFluidInCamera();
		boolean inFluid = fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW || fluid == FogType.WATER;
		boolean effectFog = camera.getEntity() instanceof LivingEntity living
				&& (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
		boolean remove;
		if (fluid == FogType.LAVA) {
			remove = config.noFogLava;
		} else if (fluid == FogType.POWDER_SNOW) {
			remove = config.noFogPowderSnow;
		} else if (effectFog) {
			remove = false;
		} else if (fluid == FogType.WATER) {
			remove = config.noFogWater;
		} else {
			remove = config.noFogDistance;
		}
		if (!remove) {
			return;
		}
		if (fogMode == FogRenderer.FogMode.FOG_SKY) {
			if (inFluid) {
				RenderSystem.setShaderFogStart(0.0F);
				RenderSystem.setShaderFogEnd(farPlaneDistance);
			}
			return;
		}
		RenderSystem.setShaderFogStart(Float.MAX_VALUE);
		RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
	}
}
