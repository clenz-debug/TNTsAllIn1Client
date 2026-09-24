package com.tntsallin1client.design;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Rasterizes view-box polygons into a {@link SpanTarget}, anti-aliased - see {@link #fillConvex}.
 * Shared by {@link ClientLogo} and {@link FeatureIcons}: both draw their shapes in their own view-box
 * units, rasterized once per pixel size into a texture.
 */
final class Shapes {
	private static final int SUBSAMPLES = 4;

	private final SpanTarget target;
	private final float originX;
	private final float originY;
	final float unit;
	private boolean roundJoins;

	Shapes(SpanTarget target, float originX, float originY, float unit) {
		this.target = target;
		this.originX = originX;
		this.originY = originY;
		this.unit = unit;
	}

	/**
	 * Round joins and caps for every stroke from here on - the default square extension leaves
	 * little spikes where a diagonal edge meets a straight one, visible on small wireframe icons.
	 */
	Shapes roundJoins() {
		this.roundJoins = true;
		return this;
	}

	/** Fills a convex view-box polygon. */
	void fill(float[] viewPoints, int color) {
		float[] points = new float[viewPoints.length];
		for (int i = 0; i < viewPoints.length; i += 2) {
			points[i] = this.originX + viewPoints[i] * this.unit;
			points[i + 1] = this.originY + viewPoints[i + 1] * this.unit;
		}
		fillConvex(points, color);
	}

	/**
	 * Fills any polygon - concave too, and with holes: every contour's edges count, even-odd rule,
	 * so a contour inside another cuts it out. Anti-aliased like {@link #fillConvex}, just slower.
	 */
	void fillEvenOdd(float[][] viewContours, int color) {
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float[][] contours = new float[viewContours.length][];
		for (int c = 0; c < viewContours.length; c++) {
			contours[c] = new float[viewContours[c].length];
			for (int i = 0; i < viewContours[c].length; i += 2) {
				float x = this.originX + viewContours[c][i] * this.unit;
				float y = this.originY + viewContours[c][i + 1] * this.unit;
				contours[c][i] = x;
				contours[c][i + 1] = y;
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}
		int left = (int) Math.floor(minX);
		float[] coverage = new float[(int) Math.ceil(maxX) - left + 1];
		float[] crossings = new float[64];
		for (int row = (int) Math.floor(minY); row < (int) Math.ceil(maxY); row++) {
			java.util.Arrays.fill(coverage, 0);
			for (int sample = 0; sample < SUBSAMPLES; sample++) {
				float sampleY = row + (sample + 0.5f) / SUBSAMPLES;
				int count = 0;
				for (float[] points : contours) {
					int vertices = points.length / 2;
					for (int i = 0; i < vertices; i++) {
						float ay = points[i * 2 + 1];
						float by = points[((i + 1) % vertices) * 2 + 1];
						if ((ay <= sampleY && by > sampleY) || (by <= sampleY && ay > sampleY)) {
							float ax = points[i * 2];
							float bx = points[((i + 1) % vertices) * 2];
							if (count == crossings.length) {
								crossings = java.util.Arrays.copyOf(crossings, count * 2);
							}
							crossings[count++] = ax + (sampleY - ay) / (by - ay) * (bx - ax);
						}
					}
				}
				java.util.Arrays.sort(crossings, 0, count);
				for (int i = 0; i + 1 < count; i += 2) {
					float from = crossings[i];
					float to = crossings[i + 1];
					for (int column = (int) Math.floor(from); column < (int) Math.ceil(to); column++) {
						coverage[column - left] += Math.max(0.0f, Math.min(column + 1, to) - Math.max(column, from));
					}
				}
			}
			for (int i = 0; i < coverage.length; i++) {
				float alpha = coverage[i] / SUBSAMPLES;
				if (alpha > 0.01f) {
					this.target.span(left + i, left + i + 1, row, ClientTheme.withAlpha(color, Math.min(1.0f, alpha)));
				}
			}
		}
	}

	/** A single straight stroke between two view-box points, same quad shape as one edge of {@link #stroke}. */
	void line(float fromX, float fromY, float toX, float toY, float width, int color) {
		stroke(new float[] {fromX, fromY, toX, toY}, width, color, false);
	}

	/** A closed polygon outline; {@code width} is in pixels. */
	void stroke(float[] viewPoints, float width, int color) {
		stroke(viewPoints, width, color, true);
	}

	/** An open polyline - arcs, curves, zigzags; {@code width} is in pixels. */
	void polyline(float[] viewPoints, float width, int color) {
		stroke(viewPoints, width, color, false);
	}

	/** Each edge as its own quad, extended by half the width at both ends so corners close cleanly - or with a disc on each vertex for {@link #roundJoins}. */
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
			if (this.roundJoins) {
				fillConvex(new float[] {x0 - dy, y0 + dx, x1 - dy, y1 + dx, x1 + dy, y1 - dx, x0 + dy, y0 - dx}, color);
				continue;
			}
			fillConvex(new float[] {
					x0 - dx - dy, y0 - dy + dx,
					x1 + dx - dy, y1 + dy + dx,
					x1 + dx + dy, y1 + dy - dx,
					x0 - dx + dy, y0 - dy - dx
			}, color);
		}
		if (this.roundJoins) {
			for (int i = 0; i < count; i++) {
				disc(this.originX + viewPoints[i * 2] * this.unit, this.originY + viewPoints[i * 2 + 1] * this.unit, width / 2, color);
			}
		}
	}

	/** Filled circle in pixel coordinates. */
	private void disc(float centerX, float centerY, float radius, int color) {
		int segments = 16;
		float[] points = new float[segments * 2];
		for (int i = 0; i < segments; i++) {
			double angle = Math.PI * 2 * i / segments;
			points[i * 2] = centerX + radius * (float) Math.cos(angle);
			points[i * 2 + 1] = centerY + radius * (float) Math.sin(angle);
		}
		fillConvex(points, color);
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

	/** Where {@link Shapes} puts its horizontal pixel spans - a texture's {@link Raster}, or the screen directly. */
	interface SpanTarget {
		void span(int x0, int x1, int row, int color);
	}

	/** Straight-alpha ARGB pixel buffer with source-over blending, so later shapes paint over earlier ones like in an SVG. */
	static final class Raster implements SpanTarget {
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

		NativeImage toImage() {
			NativeImage image = new NativeImage(this.width, this.height, true);
			for (int py = 0; py < this.height; py++) {
				for (int px = 0; px < this.width; px++) {
					image.setPixel(px, py, this.pixels[py * this.width + px]);
				}
			}
			return image;
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
}
