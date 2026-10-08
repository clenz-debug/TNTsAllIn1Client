package com.tntsallin1client.waypoint;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.render.WorldShapes;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Draws every visible waypoint of the current dimension as a colored vertical beam plus a
 * floating name (and, if enabled, live distance) label - through {@link WorldShapes} from Fabric's
 * {@code WorldRenderEvents}, the same way {@link com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer}
 * does, so no mixin is needed here either.
 *
 * <p>Everything is drawn on top - unlike the spawn overlay (which is only
 * meant to mark ground right around the player), a waypoint is supposed to help navigate towards
 * something that is usually not currently visible, so it has to render through terrain the same
 * way vanilla's own beacon beam does.
 *
 * <p>Beam and marker cuboid are each independently toggleable per waypoint ({@link Waypoint#showBeam}/
 * {@link Waypoint#showMarker}). Optional smooth fade-out as the player gets close
 * ({@link Waypoint#fadeNearby}) multiplies every color's alpha ({@link WorldShapes#multiplyAlpha}).
 *
 * <p>The label used to be plain text in the waypoint's color at a fixed world size - it shrank like a
 * name tag and got lost against sky/terrain from afar. It is now white text on a dark, camera-facing
 * backdrop outlined in the waypoint's color, and scales with distance so its on-screen size stays
 * constant (see {@link #drawLabel}).
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
	private static final float MARKER_WIDTH = 2.0F;
	// Label size: never smaller than a vanilla name tag (0.4), and past ~7 blocks growing with
	// distance so it keeps the same on-screen size instead of shrinking.
	private static final float LABEL_MIN_SCALE = 0.4F;
	private static final float LABEL_SCALE_PER_BLOCK = 0.06F;
	// Labels of farther waypoints are pulled in to this distance along the view ray - identical on
	// screen thanks to the distance scaling, but never clipped by the far plane.
	private static final double LABEL_MAX_DISTANCE = 64.0;
	private static final int LABEL_TEXT_COLOR = 0xFFFFFFFF;
	private static final int LABEL_BACKGROUND_COLOR = 0xB0000000;
	private static final float LABEL_OUTLINE_WIDTH = 2.0F;
	// Backdrop padding around the text, in font pixels.
	private static final int LABEL_PADDING = 2;
	// Also the point WaypointArrowHud tests against the field of view.
	static final float LABEL_HEIGHT_ABOVE_BLOCK = 1.5F;
	// Only used when Waypoint#fadeNearby is on: fully visible at/beyond FADE_START,
	// linearly fades between the two, fully invisible at/within FADE_END.
	private static final float FADE_START_DISTANCE = 10F;
	private static final float FADE_END_DISTANCE = 3F;

	private WaypointRenderer() {
	}

	public static void register() {
		WorldRenderEvents.LAST.register(context -> draw());
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

		String currentDimension = client.level.dimension().location().toString();
		Vec3 playerPos = client.player.position();
		Camera camera = client.gameRenderer.getMainCamera();

		for (Waypoint waypoint : config.waypointsFor(worldKey)) {
			if (!waypoint.visible || !waypoint.dimension.equals(currentDimension)) {
				continue;
			}
			drawWaypoint(waypoint, playerPos, camera);
		}
	}

	private static void drawWaypoint(Waypoint waypoint, Vec3 playerPos, Camera camera) {
		Vec3 base = new Vec3(waypoint.x + 0.5, waypoint.y, waypoint.z + 0.5);
		double distance = playerPos.distanceTo(base);

		float alpha = waypoint.fadeNearby ? fadeAlpha(distance) : 1F;
		int color = WorldShapes.multiplyAlpha(waypoint.color, alpha);
		if (FastColor.ARGB32.alpha(color) == 0) {
			return;
		}

		if (waypoint.showBeam) {
			WorldShapes.line(base, base.add(0, BEAM_HEIGHT, 0), color, BEAM_WIDTH, true);
		}
		if (waypoint.showMarker) {
			WorldShapes.box(new AABB(new BlockPos(waypoint.x, waypoint.y, waypoint.z)), color, MARKER_WIDTH, true);
		}

		String label = waypoint.showDistance
				? waypoint.name + " (" + Math.round(distance) + "m)"
				: waypoint.name;
		drawLabel(label, base.add(0, LABEL_HEIGHT_ABOVE_BLOCK, 0), color, alpha, camera);
	}

	private static void drawLabel(String text, Vec3 anchor, int outlineColor, float alpha, Camera camera) {
		Vec3 cameraPos = camera.getPosition();
		Vec3 toAnchor = anchor.subtract(cameraPos);
		double distance = toAnchor.length();
		double labelDistance = Math.min(distance, LABEL_MAX_DISTANCE);
		Vec3 pos = distance > LABEL_MAX_DISTANCE ? cameraPos.add(toAnchor.scale(LABEL_MAX_DISTANCE / distance)) : anchor;
		float scale = Math.max(LABEL_MIN_SCALE, (float) labelDistance * LABEL_SCALE_PER_BLOCK);

		// The text is drawn with one font pixel as scale / 16 world units, centered horizontally, glyph tops
		// at pos, facing the camera - so the backdrop is built in the camera's own right/up plane.
		double pixel = scale / 16.0;
		Font font = Minecraft.getInstance().font;
		double halfWidth = (font.width(text) / 2.0 + LABEL_PADDING) * pixel;
		double top = LABEL_PADDING * pixel;
		double bottom = (font.lineHeight + LABEL_PADDING - 1) * pixel;
		Vec3 up = toVec3(camera.getUpVector());
		Vec3 right = toVec3(camera.getLeftVector()).reverse();
		// Pushed half a pixel behind the text so the two never z-fight.
		Vec3 center = pos.add(toVec3(camera.getLookVector()).scale(pixel / 2));

		Vec3 topLeft = center.add(right.scale(-halfWidth)).add(up.scale(top));
		Vec3 topRight = center.add(right.scale(halfWidth)).add(up.scale(top));
		Vec3 bottomRight = center.add(right.scale(halfWidth)).add(up.scale(-bottom));
		Vec3 bottomLeft = center.add(right.scale(-halfWidth)).add(up.scale(-bottom));
		int background = WorldShapes.multiplyAlpha(LABEL_BACKGROUND_COLOR, alpha);
		WorldShapes.quad(topLeft, topRight, bottomRight, bottomLeft, background, true);
		WorldShapes.lines(LABEL_OUTLINE_WIDTH, true, sink -> {
			sink.line(topLeft, topRight, outlineColor);
			sink.line(topRight, bottomRight, outlineColor);
			sink.line(bottomRight, bottomLeft, outlineColor);
			sink.line(bottomLeft, topLeft, outlineColor);
		});

		WorldShapes.text(text, pos, WorldShapes.multiplyAlpha(LABEL_TEXT_COLOR, alpha), scale, true);
	}

	private static Vec3 toVec3(Vector3fc vector) {
		return new Vec3(vector.x(), vector.y(), vector.z());
	}

	/** 1 at/beyond {@link #FADE_START_DISTANCE}, 0 at/within {@link #FADE_END_DISTANCE}, linear between. */
	static float fadeAlpha(double distance) {
		if (distance >= FADE_START_DISTANCE) {
			return 1F;
		}
		if (distance <= FADE_END_DISTANCE) {
			return 0F;
		}
		return (float) ((distance - FADE_END_DISTANCE) / (FADE_START_DISTANCE - FADE_END_DISTANCE));
	}
}
