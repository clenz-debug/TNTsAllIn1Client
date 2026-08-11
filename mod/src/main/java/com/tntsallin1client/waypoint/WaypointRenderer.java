package com.tntsallin1client.waypoint;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

/**
 * Draws every visible waypoint of the current dimension as a colored vertical beam plus a
 * floating name (and, if enabled, live distance) label - same {@code Gizmos}/{@code WorldRenderEvents}
 * approach {@link com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer} already uses, so no
 * custom {@code VertexConsumer} code and no mixin needed here either.
 *
 * <p>Every gizmo is marked {@code setAlwaysOnTop()} - unlike the spawn overlay (which is only
 * meant to mark ground right around the player), a waypoint is supposed to help navigate towards
 * something that is usually not currently visible, so it has to render through terrain the same
 * way vanilla's own beacon beam does.
 *
 * <p>Deliberately does not attempt any Nether/Overworld coordinate-scale conversion - a waypoint
 * only ever shows while standing in the exact dimension it was created in ({@link Waypoint#dimension}
 * compared against {@code Level#dimension()}), never across dimensions. Filtered a level further up
 * too - {@link WaypointScope#currentKey} scopes the whole list to the current singleplayer save /
 * multiplayer server first, so waypoints from one world never bleed into an unrelated one.
 */
public final class WaypointRenderer {
	private static final float BEAM_HEIGHT = 256F;
	private static final float BEAM_WIDTH = 2.0F;
	private static final float LABEL_SCALE = 0.4F;
	private static final float LABEL_HEIGHT_ABOVE_BLOCK = 1.5F;

	private WaypointRenderer() {
	}

	public static void register() {
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> draw());
	}

	private static void draw() {
		ClientConfig config = ClientConfig.get();
		if (!config.waypointsEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}

		String worldKey = WaypointScope.currentKey(client);
		if (worldKey == null) {
			return;
		}

		String currentDimension = client.level.dimension().identifier().toString();
		Vec3 playerPos = client.player.position();

		for (Waypoint waypoint : config.waypointsFor(worldKey)) {
			if (!waypoint.visible || !waypoint.dimension.equals(currentDimension)) {
				continue;
			}
			drawWaypoint(waypoint, config, playerPos);
		}
	}

	private static void drawWaypoint(Waypoint waypoint, ClientConfig config, Vec3 playerPos) {
		Vec3 base = new Vec3(waypoint.x + 0.5, waypoint.y, waypoint.z + 0.5);

		if (config.waypointShowBeam) {
			Gizmos.line(base, base.add(0, BEAM_HEIGHT, 0), waypoint.color, BEAM_WIDTH).setAlwaysOnTop();
		}
		Gizmos.cuboid(new BlockPos(waypoint.x, waypoint.y, waypoint.z), GizmoStyle.stroke(waypoint.color)).setAlwaysOnTop();

		String label = config.waypointShowDistance
				? waypoint.name + " (" + Math.round(playerPos.distanceTo(base)) + "m)"
				: waypoint.name;
		Gizmos.billboardText(label, base.add(0, LABEL_HEIGHT_ABOVE_BLOCK, 0),
				TextGizmo.Style.forColorAndCentered(waypoint.color).withScale(LABEL_SCALE)).setAlwaysOnTop();
	}
}
