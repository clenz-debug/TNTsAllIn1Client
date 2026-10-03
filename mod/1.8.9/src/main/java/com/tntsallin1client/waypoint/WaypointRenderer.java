package com.tntsallin1client.waypoint;

import java.nio.FloatBuffer;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Draws every visible waypoint of the current dimension into the world: a vertical beam in its
 * color, the outline of its block, and its name (with the distance, if switched on) on a dark plate
 * framed in its color - the same three parts as in the Fabric versions of the mod. All of it shows
 * through terrain: a waypoint leads to something that usually isn't in view.
 *
 * <p>Called once the world is drawn, before the hand (see {@code GameRendererMixin}). The game draws
 * the world around the camera entity's position and has the view set up at that point; both are
 * read here every frame - the label is laid into the view's own right/up plane, and
 * {@link WaypointArrowHud} asks {@link #toCameraSpace} what is on screen.
 *
 * <p>A waypoint farther away than {@link #MAX_DRAW_DISTANCE} is drawn closer on the line from the
 * eye to it, and smaller by the same share - on screen that is the same picture, but nothing of it
 * lies beyond the distance the game draws to. The label grows with the distance instead, so it
 * stays readable.
 *
 * <p>Waypoints are only drawn in the dimension they were set in ({@link Waypoint#dimension}); there
 * is no conversion between Nether and Overworld coordinates.
 */
public final class WaypointRenderer {
	private static final float BEAM_HEIGHT = 256F;
	private static final float LINE_WIDTH = 2.0F;
	private static final double MAX_DRAW_DISTANCE = 64.0;
	/** The game draws to its view distance times the square root of 2; the beam's top stays within this share of the view distance. */
	private static final double BEAM_REACH = 1.3;
	// Label size: never smaller than a name tag, and from about 7 blocks on growing with the
	// distance, so it keeps its size on screen instead of shrinking.
	private static final float LABEL_MIN_SCALE = 0.4F;
	private static final float LABEL_SCALE_PER_BLOCK = 0.06F;
	private static final int LABEL_TEXT_COLOR = 0xFFFFFFFF;
	private static final int LABEL_BACKGROUND_COLOR = 0xB0000000;
	/** Plate around the text, in font pixels. */
	private static final int LABEL_PADDING = 2;
	/** Also the point {@link WaypointArrowHud} tests against the field of view. */
	static final float LABEL_HEIGHT_ABOVE_BLOCK = 1.5F;
	// With Waypoint#fadeNearby: fully visible from FADE_START on, gone within FADE_END, fading in between.
	private static final float FADE_START_DISTANCE = 10F;
	private static final float FADE_END_DISTANCE = 3F;
	/** The text renderer takes an alpha below this for "no alpha given" and draws fully opaque. */
	private static final int MIN_TEXT_ALPHA = 4;

	private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
	// The view as the world was last drawn with it, column by column as OpenGL keeps it.
	private static final float[] MODEL_VIEW = new float[16];
	private static final float[] PROJECTION = new float[16];
	/** The point of the world the game draws at 0/0/0: where the camera entity is this frame. */
	private static double originX;
	private static double originY;
	private static double originZ;
	/** The eye, counted from the origin - the entity's eye height, or behind/in front of it in third person. */
	private static double eyeX;
	private static double eyeY;
	private static double eyeZ;
	private static boolean viewKnown;

	private WaypointRenderer() {
	}

	public static void render(float tickDelta) {
		ClientConfig config = ClientConfig.get();
		if (!config.waypointsEnabled) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		Entity camera = client.getCameraEntity();
		if (client.world == null || client.player == null || camera == null) {
			return;
		}
		String worldKey = WaypointScope.currentKey(client);
		if (worldKey == null) {
			return;
		}

		originX = camera.prevTickX + (camera.x - camera.prevTickX) * tickDelta;
		originY = camera.prevTickY + (camera.y - camera.prevTickY) * tickDelta;
		originZ = camera.prevTickZ + (camera.z - camera.prevTickZ) * tickDelta;
		readMatrix(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW);
		readMatrix(GL11.GL_PROJECTION_MATRIX, PROJECTION);
		// The view is a rotation followed by a shift; turned back, the shift is where the eye is.
		float[] m = MODEL_VIEW;
		eyeX = -(m[0] * m[12] + m[1] * m[13] + m[2] * m[14]);
		eyeY = -(m[4] * m[12] + m[5] * m[13] + m[6] * m[14]);
		eyeZ = -(m[8] * m[12] + m[9] * m[13] + m[10] * m[14]);
		viewKnown = true;

		List<Waypoint> waypoints = config.waypointsFor(worldKey);
		if (waypoints.isEmpty()) {
			return;
		}
		String dimension = WaypointDimensions.key(client.world);
		double viewDistance = client.options.viewDistance * 16;
		double maxDistance = Math.min(MAX_DRAW_DISTANCE, viewDistance / 2);

		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		GlStateManager.disableTexture();
		GlStateManager.disableCull();
		GlStateManager.disableDepthTest();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GL11.glLineWidth(LINE_WIDTH);

		for (Waypoint waypoint : waypoints) {
			if (waypoint.visible && waypoint.dimension.equals(dimension)) {
				drawWaypoint(client, waypoint, maxDistance, viewDistance * BEAM_REACH);
			}
		}

		// Back to what the game has set at this point of drawing the world.
		GL11.glLineWidth(1.0F);
		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepthTest();
		GlStateManager.enableCull();
		GlStateManager.enableTexture();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void readMatrix(int which, float[] into) {
		MATRIX_BUFFER.clear();
		GlStateManager.getFloat(which, MATRIX_BUFFER);
		MATRIX_BUFFER.get(into);
	}

	private static void drawWaypoint(MinecraftClient client, Waypoint waypoint, double maxDistance, double beamReach) {
		double baseX = waypoint.x + 0.5;
		double baseY = waypoint.y;
		double baseZ = waypoint.z + 0.5;
		double distance = distance(client.player.x - baseX, client.player.y - baseY, client.player.z - baseZ);

		float alpha = waypoint.fadeNearby ? fadeAlpha(distance) : 1F;
		int color = withAlpha(waypoint.color, alpha);
		if ((color >>> 24) == 0) {
			return;
		}

		// Counted from the eye from here on.
		double x = baseX - originX - eyeX;
		double y = baseY - originY - eyeY;
		double z = baseZ - originZ - eyeZ;
		double eyeDistance = distance(x, y, z);
		double pull = eyeDistance > maxDistance ? maxDistance / eyeDistance : 1.0;

		if (waypoint.showBeam || waypoint.showMarker) {
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();
			buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
			if (waypoint.showBeam) {
				double beamX = x * pull;
				double beamY = y * pull;
				double beamZ = z * pull;
				double height = BEAM_HEIGHT * pull;
				double reachSquared = beamReach * beamReach - beamX * beamX - beamZ * beamZ;
				if (reachSquared > 0) {
					height = Math.min(height, Math.sqrt(reachSquared) - beamY);
				}
				if (height > 0) {
					line(buffer, beamX, beamY, beamZ, beamX, beamY + height, beamZ, color);
				}
			}
			if (waypoint.showMarker) {
				double minX = (x - 0.5) * pull;
				double minY = y * pull;
				double minZ = (z - 0.5) * pull;
				box(buffer, minX, minY, minZ, minX + pull, minY + pull, minZ + pull, color);
			}
			tessellator.draw();
		}

		String label = waypoint.showDistance ? waypoint.name + " (" + Math.round(distance) + "m)" : waypoint.name;
		drawLabel(client.textRenderer, label, x, y + LABEL_HEIGHT_ABOVE_BLOCK, z, maxDistance, color, alpha);
	}

	/** The label hangs below the given point (counted from the eye), centered on it and facing the screen. */
	private static void drawLabel(TextRenderer textRenderer, String text, double x, double y, double z, double maxDistance, int frameColor, float alpha) {
		double distance = distance(x, y, z);
		double labelDistance = Math.min(distance, maxDistance);
		double pull = distance > 0 ? labelDistance / distance : 1.0;
		// A font pixel in blocks.
		float pixel = Math.max(LABEL_MIN_SCALE, (float) labelDistance * LABEL_SCALE_PER_BLOCK) / 16F;

		// From font pixels (x to the right, y down, as on a screen) to the world: the view's own right
		// and up direction, so the label lies flat on the screen however the camera is turned.
		float[] m = MODEL_VIEW;
		MATRIX_BUFFER.clear();
		MATRIX_BUFFER.put(m[0] * pixel).put(m[4] * pixel).put(m[8] * pixel).put(0F);
		MATRIX_BUFFER.put(-m[1] * pixel).put(-m[5] * pixel).put(-m[9] * pixel).put(0F);
		MATRIX_BUFFER.put(m[2] * pixel).put(m[6] * pixel).put(m[10] * pixel).put(0F);
		MATRIX_BUFFER.put((float) (eyeX + x * pull)).put((float) (eyeY + y * pull)).put((float) (eyeZ + z * pull)).put(1F);
		MATRIX_BUFFER.flip();

		GlStateManager.pushMatrix();
		GlStateManager.multiMatrix(MATRIX_BUFFER);

		int textWidth = textRenderer.getStringWidth(text);
		double left = -textWidth / 2.0 - LABEL_PADDING;
		double right = textWidth / 2.0 + LABEL_PADDING;
		double top = -LABEL_PADDING;
		double bottom = textRenderer.fontHeight + LABEL_PADDING - 1;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		int background = withAlpha(LABEL_BACKGROUND_COLOR, alpha);
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		vertex(buffer, left, top, 0, background);
		vertex(buffer, left, bottom, 0, background);
		vertex(buffer, right, bottom, 0, background);
		vertex(buffer, right, top, 0, background);
		tessellator.draw();

		buffer.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
		vertex(buffer, left, top, 0, frameColor);
		vertex(buffer, left, bottom, 0, frameColor);
		vertex(buffer, right, bottom, 0, frameColor);
		vertex(buffer, right, top, 0, frameColor);
		tessellator.draw();

		int textColor = withAlpha(LABEL_TEXT_COLOR, alpha);
		if ((textColor >>> 24) >= MIN_TEXT_ALPHA) {
			GlStateManager.enableTexture();
			textRenderer.draw(text, -textWidth / 2, 0, textColor);
			GlStateManager.disableTexture();
		}

		GlStateManager.popMatrix();
	}

	/**
	 * Where a point of the world is as the camera sees it. Shifts {@code out} to x (to the right) and
	 * y (up) of the point, counted from the eye along the screen's own directions.
	 *
	 * @return whether the point is on screen; false as well before the world was drawn for the first time
	 */
	static boolean toCameraSpace(double worldX, double worldY, double worldZ, double[] out) {
		if (!viewKnown) {
			return false;
		}
		double x = worldX - originX;
		double y = worldY - originY;
		double z = worldZ - originZ;
		float[] m = MODEL_VIEW;
		double cameraX = m[0] * x + m[4] * y + m[8] * z + m[12];
		double cameraY = m[1] * x + m[5] * y + m[9] * z + m[13];
		double cameraZ = m[2] * x + m[6] * y + m[10] * z + m[14];
		out[0] = cameraX;
		out[1] = cameraY;

		float[] p = PROJECTION;
		double clipX = p[0] * cameraX + p[4] * cameraY + p[8] * cameraZ + p[12];
		double clipY = p[1] * cameraX + p[5] * cameraY + p[9] * cameraZ + p[13];
		double clipW = p[3] * cameraX + p[7] * cameraY + p[11] * cameraZ + p[15];
		return clipW > 0 && Math.abs(clipX) <= clipW && Math.abs(clipY) <= clipW;
	}

	private static void box(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
		// Bottom, top, then the four edges between them.
		line(buffer, minX, minY, minZ, maxX, minY, minZ, color);
		line(buffer, maxX, minY, minZ, maxX, minY, maxZ, color);
		line(buffer, maxX, minY, maxZ, minX, minY, maxZ, color);
		line(buffer, minX, minY, maxZ, minX, minY, minZ, color);
		line(buffer, minX, maxY, minZ, maxX, maxY, minZ, color);
		line(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, color);
		line(buffer, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		line(buffer, minX, maxY, maxZ, minX, maxY, minZ, color);
		line(buffer, minX, minY, minZ, minX, maxY, minZ, color);
		line(buffer, maxX, minY, minZ, maxX, maxY, minZ, color);
		line(buffer, maxX, minY, maxZ, maxX, maxY, maxZ, color);
		line(buffer, minX, minY, maxZ, minX, maxY, maxZ, color);
	}

	/** A line between two points counted from the eye. */
	private static void line(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
		vertex(buffer, eyeX + x1, eyeY + y1, eyeZ + z1, color);
		vertex(buffer, eyeX + x2, eyeY + y2, eyeZ + z2, color);
	}

	private static void vertex(BufferBuilder buffer, double x, double y, double z, int color) {
		buffer.vertex(x, y, z).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, color >>> 24).next();
	}

	private static double distance(double x, double y, double z) {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** The color with its alpha multiplied by the given share. */
	static int withAlpha(int argb, float share) {
		int alpha = Math.round((argb >>> 24) * share);
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	/** 1 from {@link #FADE_START_DISTANCE} on, 0 within {@link #FADE_END_DISTANCE}, rising evenly in between. */
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
