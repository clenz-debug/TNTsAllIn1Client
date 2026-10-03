package com.tntsallin1client.freecam;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Holds the freecam back at walls - what keeps it from being a way to look into closed-off places.
 * Doors, fence gates and trapdoors are always passable, open or closed: a player could just open
 * them and walk through, so there is nothing to gain either way. Other entities are not in the way.
 *
 * <p>An invisible border keeps the camera inside the terrain the player can actually see: chunks
 * outside the render distance around the real player, chunks the client has not been sent, and
 * everything below the world count as solid. Without it the camera could fly out and look at the
 * visible world's cut edge - caves and all - from outside.
 */
final class FreecamCollision {
	private FreecamCollision() {
	}

	/** How far of the wanted movement the camera can go, per axis. */
	static double[] resolve(MinecraftClient client, Entity camera, double dx, double dy, double dz) {
		World world = camera.world;
		Box box = camera.getBoundingBox();
		Box swept = box.stretch(dx, dy, dz);

		// One chunk of margin inside the render distance keeps the camera's own near edge on the visible side.
		int radius = Math.max(1, client.options.viewDistance - 1);
		int centerChunkX = MathHelper.floor(client.player.x) >> 4;
		int centerChunkZ = MathHelper.floor(client.player.z) >> 4;

		List<Box> colliders = new ArrayList<Box>();
		int minX = MathHelper.floor(swept.minX);
		int maxX = MathHelper.floor(swept.maxX);
		int minY = MathHelper.floor(swept.minY);
		int maxY = MathHelper.floor(swept.maxY);
		int minZ = MathHelper.floor(swept.minZ);
		int maxZ = MathHelper.floor(swept.maxZ);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int chunkDx = (x >> 4) - centerChunkX;
				int chunkDz = (z >> 4) - centerChunkZ;
				boolean beyondBorder = chunkDx * chunkDx + chunkDz * chunkDz > radius * radius || world.getChunk(x >> 4, z >> 4).isEmpty();
				for (int y = minY; y <= maxY; y++) {
					if (beyondBorder || y < 0) {
						colliders.add(new Box(x, y, z, x + 1, y + 1, z + 1));
						continue;
					}
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos);
					Block block = state.getBlock();
					if (block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapdoorBlock) {
						continue;
					}
					// Adds the block's collision boxes that touch the swept area - what the game asks for a moving entity too.
					block.appendCollisionBoxes(world, pos, state, swept, colliders, camera);
				}
			}
		}

		// Each call shortens the movement along one axis to what the collider leaves room for
		// (method_583: X, method_589: Y, method_594: Z - told apart by which sides each compares).
		for (Box collider : colliders) {
			dx = collider.method_583(box, dx);
		}
		box = box.offset(dx, 0.0, 0.0);
		for (Box collider : colliders) {
			dy = collider.method_589(box, dy);
		}
		box = box.offset(0.0, dy, 0.0);
		for (Box collider : colliders) {
			dz = collider.method_594(box, dz);
		}
		return new double[] {dx, dy, dz};
	}
}
