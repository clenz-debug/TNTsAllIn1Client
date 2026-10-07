package com.tntsallin1client.spawnoverlay;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Whether a hostile mob could spawn at a position - the game's own check for monsters on the ground
 * (zombies, skeletons, spiders, creepers, endermen all share it), worked out on the client and
 * without its dice: the real check lets a mob through with a chance that falls as it gets brighter,
 * an overlay can only say "can this ever happen". So a position counts as soon as the chance is
 * above zero. Mob caps, distance to the player and mobs with rules of their own (slimes) are left out.
 *
 * <p>The rules of this version, read from its bytecode (`NaturalSpawner#isSpawnPositionOk` for a
 * mob on the ground, `Monster#isDarkEnoughToSpawn`): the block below lets this kind of mob spawn on
 * it, the position itself and the one above are free for a mob, and the light there - the brighter
 * of block light and what the sky gives right now - is 7 or less. At night the sky gives 11 less,
 * which is what separates "always" from "only at night".
 */
public final class SpawnRiskCalculator {
	/** Stands for any hostile mob that spawns on the ground - they share the rules asked for here. */
	private static final EntityType<?> REFERENCE_MOB = EntityType.ZOMBIE;
	private static final int MAX_SPAWN_LIGHT = 7;
	private static final int NIGHT_SKY_DARKEN = 11;

	private SpawnRiskCalculator() {
	}

	/** `standPos` is where the mob would stand - the air above the ground, not the ground itself. */
	public static SpawnRisk classify(Level level, BlockPos standPos) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return SpawnRisk.NEVER;
		}
		BlockPos belowPos = standPos.below();
		if (!level.getBlockState(belowPos).isValidSpawn(level, belowPos, REFERENCE_MOB)) {
			return SpawnRisk.NEVER;
		}
		if (!isFreeForMob(level, standPos) || !isFreeForMob(level, standPos.above())) {
			return SpawnRisk.NEVER;
		}
		if (level.getMaxLocalRawBrightness(standPos, 0) <= MAX_SPAWN_LIGHT) {
			return SpawnRisk.ALWAYS;
		}
		if (level.getMaxLocalRawBrightness(standPos, NIGHT_SKY_DARKEN) <= MAX_SPAWN_LIGHT) {
			return SpawnRisk.NIGHT_ONLY;
		}
		return SpawnRisk.NEVER;
	}

	private static boolean isFreeForMob(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState());
	}
}
