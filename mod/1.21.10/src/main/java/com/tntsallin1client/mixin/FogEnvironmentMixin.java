package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DimensionOrBossFogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No fog", part 1/2: pushes the environmental fog of the chosen fog types out to infinity.
 * {@code FogRenderer#setupFog} runs only the first applicable environment of its list (lava,
 * powder snow, blindness, darkness, water, dimension or boss, atmospheric - bytecode-verified), so overriding the
 * result at the end of each environment's own {@code setupFog} removes exactly that fog type.
 * Blindness and darkness have their own environments that aren't targeted here, so those effects
 * keep working. {@code AtmosphericFogEnvironment} is the normal air fog, including rain;
 * {@code DimensionOrBossFogEnvironment} the Nether/End fog and the Wither/Ender Dragon boss fog,
 * which 1.21.11 folded into the air fog - both go with the "distance" switch here.
 *
 * <p>{@link Float#MAX_VALUE} is what vanilla's own "fog off" buffer uses: the fog shaders (vanilla's
 * and Sodium's, both checked) return zero fog for any distance at or below the start. Sodium reads
 * the values from the same {@code FogData} after this has run, so it follows along without a
 * mixin of its own. Sky and cloud fog stay vanilla for the air fog (they only shape the horizon);
 * the in-fluid environments and the dimension/boss fog tie both to their own short fog distance, so
 * those get reset to the render distance, like the air fog uses.
 */
@Mixin({AtmosphericFogEnvironment.class, DimensionOrBossFogEnvironment.class, WaterFogEnvironment.class, LavaFogEnvironment.class,
		PowderedSnowFogEnvironment.class})
public class FogEnvironmentMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void tntsallin1client$removeFog(FogData fogData, Entity entity, BlockPos pos, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		ClientConfig config = ClientConfig.get();
		if (!config.noFogEnabled) {
			return;
		}
		Object self = this;
		boolean atmospheric = self instanceof AtmosphericFogEnvironment;
		boolean remove = atmospheric || self instanceof DimensionOrBossFogEnvironment ? config.noFogDistance
				: self instanceof WaterFogEnvironment ? config.noFogWater
				: self instanceof LavaFogEnvironment ? config.noFogLava
				: config.noFogPowderSnow;
		if (!remove) {
			return;
		}
		fogData.environmentalStart = Float.MAX_VALUE;
		fogData.environmentalEnd = Float.MAX_VALUE;
		if (!atmospheric) {
			fogData.skyEnd = renderDistance;
			fogData.cloudEnd = renderDistance;
		}
	}
}
