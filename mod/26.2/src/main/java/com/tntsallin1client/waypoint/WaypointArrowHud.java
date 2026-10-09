package com.tntsallin1client.waypoint;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Small arrowhead at the screen edge for every visible waypoint that is currently outside the field
 * of view, pointing the way to turn - toggled globally via {@link ClientConfig#waypointOffscreenArrows}.
 *
 * <p>"Outside the field of view" is decided against the waypoint's label anchor (the same point
 * {@link WaypointRenderer} draws the name label at), so the arrow appears exactly when that label
 * leaves the screen. The test projects the anchor with the real vertical FOV the level is rendered
 * with ({@code Camera#getFov}, which already includes zoom, sprinting and the FOV effect
 * scale) - {@code options.fov()} alone would miss all of those.
 *
 * <p>The arrow sits where the ray from the screen center towards the waypoint's projected position
 * hits a margin inset from the screen edge. Using the camera-space x/y without dividing by depth
 * gives that same direction for waypoints in front and still the "turn this way" direction for
 * waypoints behind the camera, where a real projection would flip.
 */
public class WaypointArrowHud implements HudElement {
	// Distance of the arrow's center from the screen edge, in GUI pixels.
	private static final int EDGE_MARGIN = 10;
	// Arrowhead drawn as columns of 1px fills, tip pointing along +x before rotation:
	// ARROW_LENGTH columns, the widest ARROW_HALF_WIDTH * 2 + 1 pixels tall.
	private static final int ARROW_LENGTH = 5;
	private static final int ARROW_HALF_WIDTH = 4;
	private static final int OUTLINE_COLOR = 0xC0000000;

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.waypointsEnabled || !config.waypointOffscreenArrows) {
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

		Camera camera = client.gameRenderer.mainCamera();
		double tanHalfFov = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
		int width = guiGraphics.guiWidth();
		int height = guiGraphics.guiHeight();
		double aspect = (double) width / height;

		Vec3 cameraPos = camera.position();
		Vec3 forward = toVec3(camera.forwardVector());
		Vec3 up = toVec3(camera.upVector());
		Vec3 right = toVec3(camera.leftVector()).reverse();
		String currentDimension = client.level.dimension().identifier().toString();
		Vec3 playerPos = client.player.position();

		for (Waypoint waypoint : config.waypointsFor(worldKey)) {
			if (!waypoint.visible || !waypoint.dimension.equals(currentDimension)) {
				continue;
			}
			Vec3 base = new Vec3(waypoint.x + 0.5, waypoint.y, waypoint.z + 0.5);
			float alpha = waypoint.fadeNearby ? WaypointRenderer.fadeAlpha(playerPos.distanceTo(base)) : 1F;
			if (alpha <= 0F) {
				continue;
			}

			Vec3 toAnchor = base.add(0, WaypointRenderer.LABEL_HEIGHT_ABOVE_BLOCK, 0).subtract(cameraPos);
			double camX = toAnchor.dot(right);
			double camY = toAnchor.dot(up);
			double camZ = toAnchor.dot(forward);
			if (camZ > 0 && Math.abs(camX) <= camZ * tanHalfFov * aspect && Math.abs(camY) <= camZ * tanHalfFov) {
				continue;
			}

			// Screen y points down. Straight behind with no sideways offset: point down, i.e. "turn around".
			double dirX = camX;
			double dirY = -camY;
			double length = Math.sqrt(dirX * dirX + dirY * dirY);
			if (length < 1.0E-6) {
				dirX = 0;
				dirY = 1;
			} else {
				dirX /= length;
				dirY /= length;
			}
			drawArrow(guiGraphics, width, height, dirX, dirY, ARGB.multiplyAlpha(waypoint.color, alpha), alpha);
		}
	}

	private static void drawArrow(GuiGraphicsExtractor guiGraphics, int width, int height, double dirX, double dirY, int color, float alpha) {
		double halfWidth = width / 2.0 - EDGE_MARGIN;
		double halfHeight = height / 2.0 - EDGE_MARGIN;
		double scale = Math.min(
				Math.abs(dirX) < 1.0E-6 ? Double.MAX_VALUE : halfWidth / Math.abs(dirX),
				Math.abs(dirY) < 1.0E-6 ? Double.MAX_VALUE : halfHeight / Math.abs(dirY));

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate((float) (width / 2.0 + dirX * scale), (float) (height / 2.0 + dirY * scale));
		guiGraphics.pose().rotate((float) Math.atan2(dirY, dirX));
		fillArrowhead(guiGraphics, 1, ARGB.multiplyAlpha(OUTLINE_COLOR, alpha));
		fillArrowhead(guiGraphics, 0, color);
		guiGraphics.pose().popMatrix();
	}

	/**
	 * Triangle pointing along +x, centered on its length so it rotates around its middle. {@code grow}
	 * extends it by that many pixels on every side - drawn once grown in the outline color, then plain on top.
	 */
	private static void fillArrowhead(GuiGraphicsExtractor guiGraphics, int grow, int color) {
		int startX = -ARROW_LENGTH / 2;
		for (int column = -grow; column < ARROW_LENGTH + grow; column++) {
			int half;
			if (column < 0) {
				half = ARROW_HALF_WIDTH + grow;
			} else if (column < ARROW_LENGTH) {
				half = Math.round(ARROW_HALF_WIDTH * (ARROW_LENGTH - 1 - column) / (float) (ARROW_LENGTH - 1)) + grow;
			} else {
				half = grow - (column - ARROW_LENGTH + 1);
			}
			int x = startX + column;
			guiGraphics.fill(x, -half, x + 1, half + 1, color);
		}
	}

	private static Vec3 toVec3(Vector3fc vector) {
		return new Vec3(vector.x(), vector.y(), vector.z());
	}
}
