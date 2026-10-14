package com.tntsallin1client.freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
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
 *
 * <p>An invisible border keeps the camera inside the terrain the player can actually see: chunks
 * outside the render distance around the (real) player, unloaded chunks, and everything below the
 * world's minimum Y count as solid blocks. The render distance is what counts, not "loaded": the
 * client holds more chunks than it draws (a square of them around the player, while the render
 * distance is round), so a loaded-only check left a wide band - widest at the diagonals - where the
 * camera could fly outside the visible world and look at its cut edge, caves and all, from outside
 * (own user report with screenshot). One chunk of margin inside the render distance keeps even the
 * camera's near edge on the visible side.
 */
public final class FreecamCollision {
	private FreecamCollision() {
	}

	public static Vec3 resolve(Level level, AABB box, Vec3 desired) {
		Border border = Border.around(Minecraft.getInstance());
		AABB swept = box.expandTowards(desired);
		List<VoxelShape> colliders = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(swept)) {
			// Invisible border: the client reads unloaded chunks and everything below the world as
			// air, so without this the camera could leave the loaded area (or dive under the world)
			// and look at the loaded terrain's cut edge - caves and all - from outside. Asks the
			// chunk source, not the level: ClientLevel.hasChunk always answers true (so the border
			// never held at the render distance), while the client chunk cache only reports chunks
			// it really holds.
			int chunkX = SectionPos.blockToSectionCoord(pos.getX());
			int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
			if (pos.getY() < level.getMinY() || border.outside(chunkX, chunkZ) || !level.getChunkSource().hasChunk(chunkX, chunkZ)) {
				colliders.add(Shapes.block().move(pos.getX(), pos.getY(), pos.getZ()));
				continue;
			}
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

	/** The round render distance around the real player's chunk, one chunk of margin inside it. */
	private record Border(int centerX, int centerZ, long radiusSquared, boolean active) {
		static Border around(Minecraft mc) {
			LocalPlayer player = mc.player;
			if (player == null) {
				return new Border(0, 0, 0, false);
			}
			int radius = Math.max(1, mc.options.getEffectiveRenderDistance() - 1);
			return new Border(SectionPos.blockToSectionCoord(player.getBlockX()), SectionPos.blockToSectionCoord(player.getBlockZ()),
					(long) radius * radius, true);
		}

		boolean outside(int chunkX, int chunkZ) {
			long dx = chunkX - centerX;
			long dz = chunkZ - centerZ;
			return active && dx * dx + dz * dz > radiusSquared;
		}
	}
}
