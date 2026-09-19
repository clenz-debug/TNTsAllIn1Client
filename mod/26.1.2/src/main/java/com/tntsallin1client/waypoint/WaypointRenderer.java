package com.tntsallin1client.waypoint;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
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
 * <p>Beam and marker cuboid are each independently toggleable ({@code ClientConfig#waypointShowBeam}/
 * {@code #waypointShowMarker}). Optional smooth fade-out as the player gets close
 * ({@code #waypointFadeNearby}) reuses {@code GizmoStyle}'s own alpha-multiplication mechanism
 * ({@code ARGB.multiplyAlpha}, the same one {@code GizmoStyle#multipliedStroke}/{@code #multipliedFill}
 * use internally) rather than inventing a separate transparency path.
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
	// Only used when ClientConfig#waypointFadeNearby is on: fully visible at/beyond FADE_START,
	// linearly fades between the two, fully invisible at/within FADE_END.
	private static final float FADE_START_DISTANCE = 10F;
	private static final float FADE_END_DISTANCE = 3F;

	private WaypointRenderer() {
	}

	public static void register() {
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> draw());
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
		double distance = playerPos.distanceTo(base);

		int color = config.waypointFadeNearby ? ARGB.multiplyAlpha(waypoint.color, fadeAlpha(distance)) : waypoint.color;
		if (ARGB.alpha(color) == 0) {
			return;
		}

		if (config.waypointShowBeam) {
			Gizmos.line(base, base.add(0, BEAM_HEIGHT, 0), color, BEAM_WIDTH).setAlwaysOnTop();
		}
		if (config.waypointShowMarker) {
			Gizmos.cuboid(new BlockPos(waypoint.x, waypoint.y, waypoint.z), GizmoStyle.stroke(color)).setAlwaysOnTop();
		}

		String label = config.waypointShowDistance
				? waypoint.name + " (" + Math.round(distance) + "m)"
				: waypoint.name;
		Gizmos.billboardText(label, base.add(0, LABEL_HEIGHT_ABOVE_BLOCK, 0),
				TextGizmo.Style.forColorAndCentered(color).withScale(LABEL_SCALE)).setAlwaysOnTop();
	}

	/** 1 at/beyond {@link #FADE_START_DISTANCE}, 0 at/within {@link #FADE_END_DISTANCE}, linear between. */
	private static float fadeAlpha(double distance) {
		if (distance >= FADE_START_DISTANCE) {
			return 1F;
		}
		if (distance <= FADE_END_DISTANCE) {
			return 0F;
		}
		return (float) ((distance - FADE_END_DISTANCE) / (FADE_START_DISTANCE - FADE_END_DISTANCE));
	}
}
