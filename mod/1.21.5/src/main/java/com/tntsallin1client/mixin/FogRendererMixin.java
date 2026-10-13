package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "No fog": pushes the fog of the chosen fog types out to infinity. {@code FogRenderer#setupFog}
 * picks exactly one kind of fog in this order (read in the decompiled source): lava, powder snow,
 * blindness or darkness, water, and otherwise the normal air fog - which in 1.21.5 is the fade at
 * the edge of the loaded world, rain, the Nether/End fog and the boss fog in one. It returns start
 * and end as one value object; replacing that removes exactly the fog type that ran. Blindness and
 * darkness are left alone, so those effects keep working.
 *
 * <p>{@link Float#MAX_VALUE} is what vanilla's own {@code FogParameters.NO_FOG} starts at: the fog
 * shaders return zero fog for any distance at or below the start, and Sodium reads the same
 * parameters from the render system, so it follows along without a mixin of its own. The sky pass
 * keeps its fog for the air (it only shapes the horizon); in a fluid, where the game ties it to the
 * fluid's tiny distance, it gets what the air's sky pass uses.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
	private static void tntsallin1client$removeFog(Camera camera, FogRenderer.FogMode fogMode, Vector4f fogColor, float farPlaneDistance, boolean foggy,
			float partialTick, CallbackInfoReturnable<FogParameters> cir) {
		ClientConfig config = ClientConfig.get();
		FogParameters fog = cir.getReturnValue();
		// NO_FOG: the game's own fog switch is off, nothing to remove
		if (!config.noFogEnabled || fog == FogParameters.NO_FOG) {
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
				cir.setReturnValue(new FogParameters(0.0F, farPlaneDistance, fog.shape(), fog.red(), fog.green(), fog.blue(), fog.alpha()));
			}
			return;
		}
		cir.setReturnValue(new FogParameters(Float.MAX_VALUE, Float.MAX_VALUE, fog.shape(), fog.red(), fog.green(), fog.blue(), fog.alpha()));
	}
}
