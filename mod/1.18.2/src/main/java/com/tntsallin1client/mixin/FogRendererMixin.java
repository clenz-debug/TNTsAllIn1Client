package com.tntsallin1client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No fog": pushes the fog of the chosen fog types out to infinity. {@code FogRenderer#setupFog}
 * picks exactly one kind of fog in this order (decompiled 1.18.2 source): lava, powder snow, blindness,
 * water, and otherwise the normal air fog - which in 1.18.2 is the fade at the edge of the loaded
 * world, rain, the Nether/End fog and the boss fog in one. At its end it hands start and end to the
 * render system; overriding them there removes exactly the fog type that ran. Blindness is left
 * alone, so the effect keeps working (darkness only exists from 1.19 on).
 *
 * <p>Start 10 million, end 20 million blocks - far beyond any render distance. Vanilla's fog shader
 * returns zero fog for any distance at or below the start. Sodium reads the same two values, but
 * Sodium 0.4 (the line for 1.18.2 and 1.19.x) computes its terrain fog as {@code smoothstep(end, start, distance)}
 * without that check: with start and end equal (both {@link Float#MAX_VALUE}, the newer versions' value)
 * that is undefined, and even with {@code Float.MAX_VALUE / 2} the GPU divides by a span whose inverse
 * is a denormal and flushes to zero - either way the whole terrain came out in fog colour (own user
 * report, 1.19.2, twice). Ordinary large numbers keep the shader's division exact.
 *
 * <p>The sky pass keeps its fog for the air (it only shapes the horizon); in a fluid, where the game
 * ties it to the fluid's tiny distance, it gets what the air's sky pass uses.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Unique
	private static final float NO_FOG_START = 1.0E7F;

	@Unique
	private static final float NO_FOG_END = 2.0E7F;

	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void tntsallin1client$removeFog(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog,
			CallbackInfo ci) {
		ClientConfig config = ClientConfig.get();
		if (!config.noFogEnabled) {
			return;
		}
		FogType fluid = camera.getFluidInCamera();
		boolean inFluid = fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW || fluid == FogType.WATER;
		boolean effectFog = camera.getEntity() instanceof LivingEntity living
				&& living.hasEffect(MobEffects.BLINDNESS);
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
		RenderSystem.setShaderFogStart(NO_FOG_START);
		RenderSystem.setShaderFogEnd(NO_FOG_END);
	}
}
