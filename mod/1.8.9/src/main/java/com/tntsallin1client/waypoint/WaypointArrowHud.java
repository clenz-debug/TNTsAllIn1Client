package com.tntsallin1client.waypoint;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;

/**
 * A small arrowhead at the edge of the screen for every visible waypoint that is outside the field
 * of view, pointing the way to turn - switched on and off for all waypoints together
 * ({@link ClientConfig#waypointOffscreenArrows}).
 *
 * <p>"Outside the field of view" is decided for the point the waypoint's label hangs from, with the
 * view the world was just drawn with (zoom, sprinting and third person included) - so the arrow
 * appears exactly when the label leaves the screen.
 *
 * <p>The arrow sits where the line from the screen's center towards the waypoint meets a margin
 * inside the screen's edge. The direction is the waypoint's place to the right of and above the eye,
 * not its place on screen: that is the same for a waypoint in front, and still the way to turn for
 * one behind the camera, where its place on screen would be mirrored.
 */
public final class WaypointArrowHud {
	/** Distance of the arrow's center from the screen edge. */
	private static final int EDGE_MARGIN = 10;
	// The arrowhead is drawn as columns one pixel wide, its tip pointing to the right before it is
	// turned: ARROW_LENGTH columns, the widest ARROW_HALF_WIDTH * 2 + 1 pixels tall.
	private static final int ARROW_LENGTH = 5;
	private static final int ARROW_HALF_WIDTH = 4;
	private static final int OUTLINE_COLOR = 0xC0000000;

	private WaypointArrowHud() {
	}

	/** Called with the game's HUD; `width` and `height` are the screen's size in the HUD's units. */
	public static void render(MinecraftClient client, ClientConfig config, int width, int height) {
		if (!config.waypointsEnabled || !config.waypointOffscreenArrows) {
			return;
		}
		if (client.world == null || client.player == null) {
			return;
		}
		String worldKey = WaypointScope.currentKey(client);
		if (worldKey == null) {
			return;
		}

		String dimension = WaypointDimensions.key(client.world);
		double[] place = new double[2];
		for (Waypoint waypoint : config.waypointsFor(worldKey)) {
			if (!waypoint.visible || !waypoint.dimension.equals(dimension)) {
				continue;
			}
			double baseX = waypoint.x + 0.5;
			double baseY = waypoint.y;
			double baseZ = waypoint.z + 0.5;
			double deltaX = client.player.x - baseX;
			double deltaY = client.player.y - baseY;
			double deltaZ = client.player.z - baseZ;
			float alpha = waypoint.fadeNearby ? WaypointRenderer.fadeAlpha(Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)) : 1F;
			if (alpha <= 0F) {
				continue;
			}
			if (WaypointRenderer.toCameraSpace(baseX, baseY + WaypointRenderer.LABEL_HEIGHT_ABOVE_BLOCK, baseZ, place)) {
				continue;
			}

			// On screen y points down. Straight behind, neither left nor right: the arrow points down - "turn around".
			double directionX = place[0];
			double directionY = -place[1];
			double length = Math.sqrt(directionX * directionX + directionY * directionY);
			if (length < 1.0E-6) {
				directionX = 0;
				directionY = 1;
			} else {
				directionX /= length;
				directionY /= length;
			}
			drawArrow(width, height, directionX, directionY, WaypointRenderer.withAlpha(waypoint.color, alpha), alpha);
		}
	}

	private static void drawArrow(int width, int height, double directionX, double directionY, int color, float alpha) {
		double halfWidth = width / 2.0 - EDGE_MARGIN;
		double halfHeight = height / 2.0 - EDGE_MARGIN;
		double scale = Math.min(
				Math.abs(directionX) < 1.0E-6 ? Double.MAX_VALUE : halfWidth / Math.abs(directionX),
				Math.abs(directionY) < 1.0E-6 ? Double.MAX_VALUE : halfHeight / Math.abs(directionY));

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (width / 2.0 + directionX * scale), (float) (height / 2.0 + directionY * scale), 0.0F);
		GlStateManager.rotate((float) Math.toDegrees(Math.atan2(directionY, directionX)), 0.0F, 0.0F, 1.0F);
		fillArrowhead(1, WaypointRenderer.withAlpha(OUTLINE_COLOR, alpha));
		fillArrowhead(0, color);
		GlStateManager.popMatrix();
	}

	/**
	 * A triangle pointing to the right, centered on its length so it turns around its middle. `grow`
	 * makes it that many pixels larger on every side - drawn once grown in the outline color, then
	 * plain on top.
	 */
	private static void fillArrowhead(int grow, int color) {
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
			DrawableHelper.fill(x, -half, x + 1, half + 1, color);
		}
	}
}
