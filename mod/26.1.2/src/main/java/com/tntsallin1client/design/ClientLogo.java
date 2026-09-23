package com.tntsallin1client.design;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The octagon "N" client logo, drawn from the exact same shapes as the launcher's {@code Logo.tsx}
 * (600x660 view box; octagon outline, beam outlines and text in background2, the four beams in
 * accent1-4) - so the in-game logo follows the launcher's theme colors the same way (own wishlist
 * item: "Logo anpassen auch an die farben").
 *
 * <p>The shapes are rasterized (anti-aliased, see {@link Shapes#fillConvex}) on the CPU into a
 * texture at the exact pixel size needed, once per size, and then drawn as a single image - drawing
 * them as per-pixel-row fills every frame took tens of thousands of quads per frame and made the
 * title screen noticeably sluggish (own user report: the mouse felt slow over the logo). Only the
 * switch animation, whose logo changes size every frame, rasterizes anew per frame.
 */
public final class ClientLogo {
	/** View-box size of {@code Logo.tsx} - every coordinate below is in these units. */
	public static final float VIEW_WIDTH = 600.0f;
	public static final float VIEW_HEIGHT = 660.0f;

	private static final float[] OCTAGON = {200, 40, 400, 40, 560, 200, 560, 400, 400, 560, 200, 560, 40, 400, 40, 200};
	private static final float[] TOP_BEAM = {150, 150, 450, 150, 450, 200, 150, 200};
	private static final float[] LEFT_LEG = {185, 200, 230, 200, 230, 440, 185, 440};
	private static final float[] RIGHT_LEG = {370, 200, 415, 200, 415, 440, 370, 440};
	private static final float[] DIAGONAL = {185, 200, 230, 200, 415, 415, 415, 440, 370, 440, 185, 225};

	/** Rendered logo sizes kept as textures - resting size, hover size and a few animation frames. */
	private static final int CACHE_SIZE = 6;
	private static final Map<Integer, CachedTexture> CACHE = new LinkedHashMap<>(16, 0.75f, true);

	private record CachedTexture(Identifier id, int width, int height) {
	}

	private ClientLogo() {
	}

	/** Logo width for a given height, keeping the view box's aspect ratio. */
	public static float widthFor(float height) {
		return height * VIEW_WIDTH / VIEW_HEIGHT;
	}

	/** Draws the logo with its top-left corner at ({@code x}, {@code y}) in GUI coordinates, {@code height} tall. */
	public static void draw(GuiGraphicsExtractor graphics, float x, float y, float height, float alpha) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		CachedTexture texture = texture(Math.max(1, Math.round(height * guiScale)));

		// Pixel-scaled pose, so the texture lands 1:1 on screen pixels - no filtering blur.
		graphics.pose().pushMatrix();
		graphics.pose().scale(1.0f / guiScale, 1.0f / guiScale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id, Math.round(x * guiScale), Math.round(y * guiScale),
				0.0f, 0.0f, texture.width, texture.height, texture.width, texture.height, ARGB.white(alpha));
		graphics.pose().popMatrix();

		int line = ClientTheme.withAlpha(ClientTheme.get().background2, alpha);
		Font font = Minecraft.getInstance().font;
		// Logo.tsx: "A I 1" at font-size 72 (baseline 500), "CLIENT" at 26 (baseline 535) - scaled so
		// the client font's ~7px cap height matches those cap heights (~0.72em). "CLIENT" only once
		// it's big enough to still be legible.
		drawText(graphics, font, "A I 1", x + widthFor(height) / 2, y + height * 448 / VIEW_HEIGHT, height * 52 / VIEW_HEIGHT / 7.0f, line);
		if (height >= 48) {
			drawText(graphics, font, "CLIENT", x + widthFor(height) / 2, y + height * 516 / VIEW_HEIGHT, height * 19 / VIEW_HEIGHT / 7.0f, line);
		}
	}

	/**
	 * An "X" icon of two anti-aliased strokes, exactly centered on ({@code centerX}, {@code centerY}) -
	 * the client layout's quit button (own user report: a text "X" never sat quite centered in its box).
	 * Small enough to draw as fills directly.
	 */
	public static void drawCross(GuiGraphicsExtractor graphics, float centerX, float centerY, float size, float thickness, int color) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		graphics.pose().pushMatrix();
		graphics.pose().scale(1.0f / guiScale, 1.0f / guiScale);
		Shapes shapes = new Shapes((x0, x1, row, spanColor) -> graphics.fill(x0, row, x1, row + 1, spanColor),
				centerX * guiScale, centerY * guiScale, guiScale);
		float half = size / 2;
		shapes.line(-half, -half, half, half, thickness * guiScale, color);
		shapes.line(-half, half, half, -half, thickness * guiScale, color);
		graphics.pose().popMatrix();
	}

	private static void drawText(GuiGraphicsExtractor graphics, Font font, String text, float centerX, float top, float scale, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, top);
		graphics.pose().scale(scale, scale);
		var label = ClientFont.of(text);
		graphics.text(font, label, -font.width(label) / 2, 0, color, false);
		graphics.pose().popMatrix();
	}

	/** The logo shapes at {@code pixelHeight}, from the cache or freshly rasterized and uploaded. */
	private static CachedTexture texture(int pixelHeight) {
		CachedTexture cached = CACHE.get(pixelHeight);
		if (cached != null) {
			return cached;
		}

		int pixelWidth = Math.max(1, (int) Math.ceil(widthFor(pixelHeight)));
		Raster raster = new Raster(pixelWidth, pixelHeight);
		Shapes shapes = new Shapes(raster, 0, 0, pixelHeight / VIEW_HEIGHT);
		float outlineWidth = Math.max(1.0f, 4.0f * shapes.unit);
		float beamOutlineWidth = Math.max(1.0f, 3.0f * shapes.unit);
		ClientTheme theme = ClientTheme.get();

		// Same paint order as the SVG: each beam filled and then outlined before the next one, so the
		// diagonal (last) covers both legs including their outlines, exactly like N-Logo.svg.
		shapes.stroke(OCTAGON, outlineWidth, theme.background2);
		shapes.fill(TOP_BEAM, theme.accent1);
		shapes.stroke(TOP_BEAM, beamOutlineWidth, theme.background2);
		shapes.fill(LEFT_LEG, theme.accent2);
		shapes.stroke(LEFT_LEG, beamOutlineWidth, theme.background2);
		shapes.fill(RIGHT_LEG, theme.accent3);
		shapes.stroke(RIGHT_LEG, beamOutlineWidth, theme.background2);
		shapes.fill(DIAGONAL, theme.accent4);
		shapes.stroke(DIAGONAL, beamOutlineWidth, theme.background2);

		NativeImage image = new NativeImage(pixelWidth, pixelHeight, true);
		for (int py = 0; py < pixelHeight; py++) {
			for (int px = 0; px < pixelWidth; px++) {
				image.setPixel(px, py, raster.pixels[py * pixelWidth + px]);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath("tntsallin1client", "dynamic/client_logo_" + pixelHeight);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "TNT's All-In-1 client logo", image));

		CachedTexture created = new CachedTexture(id, pixelWidth, pixelHeight);
		CACHE.put(pixelHeight, created);
		if (CACHE.size() > CACHE_SIZE) {
			Iterator<CachedTexture> eldest = CACHE.values().iterator();
			Minecraft.getInstance().getTextureManager().release(eldest.next().id);
			eldest.remove();
		}
		return created;
	}

	/** Where {@link Shapes} puts its horizontal pixel spans - the logo texture, or the screen directly. */
	private interface SpanTarget {
		void span(int x0, int x1, int row, int color);
	}

	/** Straight-alpha ARGB pixel buffer with source-over blending, so later shapes paint over earlier ones like in the SVG. */
	private static final class Raster implements SpanTarget {
		final int width;
		final int height;
		final int[] pixels;

		Raster(int width, int height) {
			this.width = width;
			this.height = height;
			this.pixels = new int[width * height];
		}

		@Override
		public void span(int x0, int x1, int row, int color) {
			if (row < 0 || row >= this.height) {
				return;
			}
			for (int x = Math.max(0, x0); x < Math.min(this.width, x1); x++) {
				int index = row * this.width + x;
				this.pixels[index] = blend(this.pixels[index], color);
			}
		}

		private static int blend(int destination, int source) {
			float sourceAlpha = (source >>> 24) / 255.0f;
			if (sourceAlpha <= 0) {
				return destination;
			}
			float destinationAlpha = (destination >>> 24) / 255.0f;
			float outAlpha = sourceAlpha + destinationAlpha * (1 - sourceAlpha);
			int red = channel(source >> 16, destination >> 16, sourceAlpha, destinationAlpha, outAlpha);
			int green = channel(source >> 8, destination >> 8, sourceAlpha, destinationAlpha, outAlpha);
			int blue = channel(source, destination, sourceAlpha, destinationAlpha, outAlpha);
			return (Math.round(outAlpha * 255) << 24) | (red << 16) | (green << 8) | blue;
		}

		private static int channel(int source, int destination, float sourceAlpha, float destinationAlpha, float outAlpha) {
			float value = ((source & 0xFF) * sourceAlpha + (destination & 0xFF) * destinationAlpha * (1 - sourceAlpha)) / outAlpha;
			return Math.min(255, Math.round(value));
		}
	}

	/** Rasterizes view-box polygons into a {@link SpanTarget}, anti-aliased - see {@link #fillConvex}. */
	private static final class Shapes {
		private static final int SUBSAMPLES = 4;

		private final SpanTarget target;
		private final float originX;
		private final float originY;
		final float unit;

		Shapes(SpanTarget target, float originX, float originY, float unit) {
			this.target = target;
			this.originX = originX;
			this.originY = originY;
			this.unit = unit;
		}

		void fill(float[] viewPoints, int color) {
			float[] points = new float[viewPoints.length];
			for (int i = 0; i < viewPoints.length; i += 2) {
				points[i] = this.originX + viewPoints[i] * this.unit;
				points[i + 1] = this.originY + viewPoints[i + 1] * this.unit;
			}
			fillConvex(points, color);
		}

		/** A single straight stroke between two view-box points, same quad shape as one edge of {@link #stroke}. */
		void line(float fromX, float fromY, float toX, float toY, float width, int color) {
			stroke(new float[] {fromX, fromY, toX, toY}, width, color, false);
		}

		void stroke(float[] viewPoints, float width, int color) {
			stroke(viewPoints, width, color, true);
		}

		/** Each edge as its own quad, extended by half the width at both ends so corners close cleanly. */
		private void stroke(float[] viewPoints, float width, int color, boolean closed) {
			int count = viewPoints.length / 2;
			int edges = closed ? count : count - 1;
			for (int i = 0; i < edges; i++) {
				float x0 = this.originX + viewPoints[i * 2] * this.unit;
				float y0 = this.originY + viewPoints[i * 2 + 1] * this.unit;
				float x1 = this.originX + viewPoints[((i + 1) % count) * 2] * this.unit;
				float y1 = this.originY + viewPoints[((i + 1) % count) * 2 + 1] * this.unit;
				float length = (float) Math.hypot(x1 - x0, y1 - y0);
				if (length == 0) {
					continue;
				}
				float dx = (x1 - x0) / length * width / 2;
				float dy = (y1 - y0) / length * width / 2;
				fillConvex(new float[] {
						x0 - dx - dy, y0 - dy + dx,
						x1 + dx - dy, y1 + dy + dx,
						x1 + dx + dy, y1 + dy - dx,
						x0 - dx + dy, y0 - dy - dx
				}, color);
			}
		}

		/**
		 * Anti-aliased scanline fill (own user report: the diagonal edges looked too pixelated). Each
		 * pixel row is sampled at {@link #SUBSAMPLES} sub-rows; columns covered by every sub-row are
		 * filled in one span, and the edge columns get their exact covered fraction as alpha - so the
		 * diagonal beam, its outlines and the octagon come out smooth instead of stair-stepped.
		 */
		private void fillConvex(float[] points, int color) {
			float minY = Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;
			for (int i = 1; i < points.length; i += 2) {
				minY = Math.min(minY, points[i]);
				maxY = Math.max(maxY, points[i]);
			}
			int count = points.length / 2;
			float[] lefts = new float[SUBSAMPLES];
			float[] rights = new float[SUBSAMPLES];
			for (int row = (int) Math.floor(minY); row < (int) Math.ceil(maxY); row++) {
				int covered = 0;
				float minLeft = Float.MAX_VALUE;
				float maxLeft = -Float.MAX_VALUE;
				float minRight = Float.MAX_VALUE;
				float maxRight = -Float.MAX_VALUE;
				for (int sample = 0; sample < SUBSAMPLES; sample++) {
					float sampleY = row + (sample + 0.5f) / SUBSAMPLES;
					float left = Float.MAX_VALUE;
					float right = -Float.MAX_VALUE;
					for (int i = 0; i < count; i++) {
						float ax = points[i * 2];
						float ay = points[i * 2 + 1];
						float bx = points[((i + 1) % count) * 2];
						float by = points[((i + 1) % count) * 2 + 1];
						if ((ay <= sampleY && by > sampleY) || (by <= sampleY && ay > sampleY)) {
							float crossX = ax + (sampleY - ay) / (by - ay) * (bx - ax);
							left = Math.min(left, crossX);
							right = Math.max(right, crossX);
						}
					}
					if (left > right) {
						lefts[sample] = Float.NaN;
						continue;
					}
					lefts[sample] = left;
					rights[sample] = right;
					covered++;
					minLeft = Math.min(minLeft, left);
					maxLeft = Math.max(maxLeft, left);
					minRight = Math.min(minRight, right);
					maxRight = Math.max(maxRight, right);
				}
				if (covered == 0) {
					continue;
				}

				int fullStart = 0;
				int fullEnd = 0;
				if (covered == SUBSAMPLES) {
					fullStart = (int) Math.ceil(maxLeft);
					fullEnd = (int) Math.floor(minRight);
					if (fullEnd > fullStart) {
						this.target.span(fullStart, fullEnd, row, color);
					}
				}
				for (int column = (int) Math.floor(minLeft); column < (int) Math.ceil(maxRight); column++) {
					if (fullEnd > fullStart && column >= fullStart && column < fullEnd) {
						column = fullEnd - 1;
						continue;
					}
					float coverage = 0;
					for (int sample = 0; sample < SUBSAMPLES; sample++) {
						if (!Float.isNaN(lefts[sample])) {
							coverage += Math.max(0.0f, Math.min(column + 1, rights[sample]) - Math.max(column, lefts[sample]));
						}
					}
					coverage /= SUBSAMPLES;
					if (coverage > 0.01f) {
						this.target.span(column, column + 1, row, ClientTheme.withAlpha(color, coverage));
					}
				}
			}
		}
	}
}
