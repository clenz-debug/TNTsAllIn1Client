package com.tntsallin1client.spawnoverlay;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

/**
 * Whether a hostile mob could spawn at a position - the game's own check for monsters on the ground
 * (zombies, skeletons, spiders, creepers, endermen all share it), worked out on the client and
 * without its dice: the real check lets a mob through with a chance that falls as it gets brighter,
 * an overlay can only say "can this ever happen". So a position counts as soon as the chance is
 * above zero. Mob caps, distance to the player and mobs with rules of their own (slimes) are left out.
 *
 * <p>The rules of this version, read from its bytecode: the block below has a solid top (a full
 * block, an upper slab, upside-down stairs) and is neither bedrock nor a barrier; the position
 * itself and the one above hold no full block, the position no liquid; and the light there - the
 * brighter of block light and sky light - is 7 or less. At night the sky gives 11 less, which is
 * what separates "always" from "only at night".
 */
public final class SpawnRiskCalculator {
	private static final int MAX_SPAWN_LIGHT = 7;
	private static final int NIGHT_SKY_DARKEN = 11;

	private SpawnRiskCalculator() {
	}

	/** `standPos` is where the mob would stand - the air above the ground, not the ground itself. */
	public static SpawnRisk classify(World world, BlockPos standPos) {
		if (world.getGlobalDifficulty() == Difficulty.PEACEFUL) {
			return SpawnRisk.NEVER;
		}

		BlockPos belowPos = standPos.down();
		// Named "isOpaque" in this version's name table; what it checks is the solid top.
		if (!World.isOpaque(world, belowPos)) {
			return SpawnRisk.NEVER;
		}
		Block below = world.getBlockState(belowPos).getBlock();
		if (below == Blocks.BEDROCK || below == Blocks.BARRIER) {
			return SpawnRisk.NEVER;
		}
		Block at = world.getBlockState(standPos).getBlock();
		if (at.isNormalBlock() || at.getMaterial().isFluid() || world.getBlockState(standPos.up()).getBlock().isNormalBlock()) {
			return SpawnRisk.NEVER;
		}

		int blockLight = world.getLightAtPos(LightType.BLOCK, standPos);
		int skyLight = world.getLightAtPos(LightType.SKY, standPos);
		if (Math.max(blockLight, skyLight) <= MAX_SPAWN_LIGHT) {
			return SpawnRisk.ALWAYS;
		}
		if (Math.max(blockLight, skyLight - NIGHT_SKY_DARKEN) <= MAX_SPAWN_LIGHT) {
			return SpawnRisk.NIGHT_ONLY;
		}
		return SpawnRisk.NEVER;
	}
}
