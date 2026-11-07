package com.tntsallin1client.spawnoverlay;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;

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
	private static final int LIGHT_TEST_MAX = 7;

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

		// 1.17.1 still has the old rule: Monster#isDarkEnoughToSpawn tests only the raw brightness (block
		// light included) against random.nextInt(8), in every dimension - block light up to 7 still lets
		// mobs spawn (decompiled 1.17.1 source; 1.18 added the "no block light at all" check).
		int lightTestMax = LIGHT_TEST_MAX;
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
