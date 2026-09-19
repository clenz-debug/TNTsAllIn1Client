package com.tntsallin1client.freecam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves freecam movement against real block collision - the anti-abuse core of the feature,
 * see {@link FreecamHandler}. Doors, fence gates and trapdoors are always passable, open or
 * closed, since a real player could just open and walk through them anyway - no actual
 * information/access advantage either way.
 *
 * <p>Deliberately not using the convenience {@code Entity.collideBoundingBox}/{@code
 * collectAllColliders}: their private collider-gathering helper unconditionally re-fetches every
 * block's real collision shape regardless of what's passed in, so a closed door's solid shape
 * would leak back in no matter how the input list is filtered. Collecting shapes manually and
 * resolving with {@link Shapes#collide} per axis - the same primitive those convenience methods
 * are themselves built on - is the only way to actually skip specific blocks. Entity-entity
 * collision (other players/mobs) is intentionally not checked - passing through them isn't a
 * wall-hack concern.
 */
public final class FreecamCollision {
	private FreecamCollision() {
	}

	public static Vec3 resolve(Level level, AABB box, Vec3 desired) {
		AABB swept = box.expandTowards(desired);
		List<VoxelShape> colliders = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(swept)) {
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.DOORS, s -> true)
					|| state.is(BlockTags.FENCE_GATES, s -> true)
					|| state.is(BlockTags.TRAPDOORS, s -> true)) {
				continue;
			}
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (!shape.isEmpty()) {
				colliders.add(shape.move(pos.getX(), pos.getY(), pos.getZ()));
			}
		}

		double dx = Shapes.collide(Direction.Axis.X, box, colliders, desired.x);
		AABB afterX = box.move(dx, 0, 0);
		double dy = Shapes.collide(Direction.Axis.Y, afterX, colliders, desired.y);
		AABB afterY = afterX.move(0, dy, 0);
		double dz = Shapes.collide(Direction.Axis.Z, afterY, colliders, desired.z);
		return new Vec3(dx, dy, dz);
	}
}
