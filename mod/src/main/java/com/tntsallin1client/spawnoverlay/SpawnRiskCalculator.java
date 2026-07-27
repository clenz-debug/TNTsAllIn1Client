package com.tntsallin1client.spawnoverlay;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Reimplements the light/position part of vanilla's hostile-mob spawn check
 * ({@code Monster#isDarkEnoughToSpawn}/{@code SpawnPlacementTypes#ON_GROUND},
 * decompiled and verified against 1.21.11 sources) as a deterministic,
 * client-only classification instead of the real, randomized, server-only
 * check - a static overlay can't show "there's a 3-in-8 chance a mob spawns
 * here", so it collapses that into "could this ever happen" (worst case) and
 * ignores things a client-only feature has no business simulating anyway
 * (mob caps, per-chunk spawn attempts, distance to the player).
 *
 * <p>Uses {@link EntityType#ZOMBIE} as a stand-in for "a generic hostile
 * ground mob" - {@code SpawnPlacements} registers zombies, skeletons,
 * spiders, creepers, endermen etc. with the exact same placement type
 * ({@code ON_GROUND}) and predicate ({@code Monster::checkMonsterSpawnRules}),
 * so the position/light rules are identical across all of them; this is not
 * meant to answer "can THIS SPECIFIC mob spawn here" for mobs with their own
 * extra rules (slimes, phantoms, drowned, ...).
 *
 * <p>Day vs. night: vanilla's actual "how dark is it at night" value is
 * driven by a keyframed environment-attribute track now
 * ({@code Timelines.NIGHT_SKY_LIGHT_LEVEL = 4.0F} against a default/day value
 * of {@code 15.0F}, both confirmed in the decompiled 1.21.11 sources), not a
 * simple hardcoded constant. That resolves to the exact same sky-darkening
 * amount (15 - 4 = 11) vanilla has used for many versions, so {@link #NIGHT_SKY_DARKEN}
 * is hardcoded here rather than reaching into that (internal, datagen-facing)
 * class at runtime.
 */
public final class SpawnRiskCalculator {
	private static final EntityType<?> REFERENCE_MOB = EntityType.ZOMBIE;
	private static final int NIGHT_SKY_DARKEN = 11;

	private SpawnRiskCalculator() {
	}

	/** {@code standPos} is the position a mob would occupy - i.e. the air block above the ground, not the ground itself. */
	public static SpawnRisk classify(Level level, BlockPos standPos) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return SpawnRisk.NEVER;
		}

		BlockPos belowPos = standPos.below();
		BlockState belowState = level.getBlockState(belowPos);
		if (!belowState.isValidSpawn(level, belowPos, REFERENCE_MOB)) {
			return SpawnRisk.NEVER;
		}

		if (!isValidEmptySpawnBlock(level, standPos) || !isValidEmptySpawnBlock(level, standPos.above())) {
			return SpawnRisk.NEVER;
		}

		DimensionType dimensionType = level.dimensionType();
		int blockLightLimit = dimensionType.monsterSpawnBlockLightLimit();
		int blockLight = level.getBrightness(LightLayer.BLOCK, standPos);
		if (blockLightLimit < 15 && blockLight > blockLightLimit) {
			return SpawnRisk.NEVER;
		}

		int lightTestMax = dimensionType.monsterSpawnLightTest().getMaxValue();
		int rawBrightnessDay = level.getMaxLocalRawBrightness(standPos, 0);
		if (rawBrightnessDay <= lightTestMax) {
			return SpawnRisk.ALWAYS;
		}

		int rawBrightnessNight = level.getMaxLocalRawBrightness(standPos, NIGHT_SKY_DARKEN);
		if (rawBrightnessNight <= lightTestMax) {
			return SpawnRisk.NIGHT_ONLY;
		}

		return SpawnRisk.NEVER;
	}

	private static boolean isValidEmptySpawnBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(), REFERENCE_MOB);
	}
}
