package com.tntsallin1client.freecam;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
	static double[] resolve(Minecraft client, FreecamCamera camera, double dx, double dy, double dz) {
		Level world = camera.level;
		AABB box = camera.getBoundingBox();
		AABB swept = box.expandTowards(dx, dy, dz);

		// One chunk of margin inside the render distance keeps the camera's own near edge on the visible side.
		int radius = Math.max(1, client.options.renderDistance - 1);
		int centerChunkX = Mth.floor(client.player.x) >> 4;
		int centerChunkZ = Mth.floor(client.player.z) >> 4;

		List<VoxelShape> colliders = new ArrayList<VoxelShape>();
		int minX = Mth.floor(swept.minX);
		int maxX = Mth.floor(swept.maxX);
		int minY = Mth.floor(swept.minY);
		int maxY = Mth.floor(swept.maxY);
		int minZ = Mth.floor(swept.minZ);
		int maxZ = Mth.floor(swept.maxZ);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int chunkDx = (x >> 4) - centerChunkX;
				int chunkDz = (z >> 4) - centerChunkZ;
				boolean beyondBorder = chunkDx * chunkDx + chunkDz * chunkDz > radius * radius || !world.hasChunk(x >> 4, z >> 4);
				for (int y = minY; y <= maxY; y++) {
					if (beyondBorder || y < 0) {
						colliders.add(Shapes.block().move(x, y, z));
						continue;
					}
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos);
					Block block = state.getBlock();
					if (block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapDoorBlock) {
						continue;
					}
					// The block's collision shape, moved from the block's own corner to where the block is.
					VoxelShape shape = state.getCollisionShape(world, pos);
					if (!shape.isEmpty()) {
						colliders.add(shape.move(x, y, z));
					}
				}
			}
		}

		// Each call shortens the movement along one axis to what the colliders leave room for.
		dx = Shapes.collide(Direction.Axis.X, box, colliders.stream(), dx);
		box = box.move(dx, 0.0, 0.0);
		dy = Shapes.collide(Direction.Axis.Y, box, colliders.stream(), dy);
		box = box.move(0.0, dy, 0.0);
		dz = Shapes.collide(Direction.Axis.Z, box, colliders.stream(), dz);
		return new double[] {dx, dy, dz};
	}
}
