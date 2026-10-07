package com.tntsallin1client.crosshair;

import net.minecraft.client.gui.GuiComponent;

/**
 * A crosshair (preset or drawn by the player) is a small square grid of on/off pixels - the same
 * representation for both, so one render routine covers everything. {@link #circleRing} and
 * {@link #squareRing} generate the ring presets instead of 81 hand-set booleans per shape.
 */
public final class CrosshairGrid {
	public static final int SIZE = 9;
	public static final int CENTER = 4;

	private CrosshairGrid() {
	}

	public static boolean[][] empty() {
		return new boolean[SIZE][SIZE];
	}

	public static boolean[][] copy(boolean[][] source) {
		boolean[][] copy = new boolean[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++) {
			System.arraycopy(source[row], 0, copy[row], 0, SIZE);
		}
		return copy;
	}

	public static boolean[][] circleRing(float radius) {
		boolean[][] grid = empty();
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				float dx = col - CENTER;
				float dy = row - CENTER;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				if (Math.abs(dist - radius) < 0.75F) {
					grid[row][col] = true;
				}
			}
		}
		return grid;
	}

	public static boolean[][] squareRing(int radius) {
		boolean[][] grid = empty();
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				int dx = Math.abs(col - CENTER);
				int dy = Math.abs(row - CENTER);
				if (Math.max(dx, dy) == radius) {
					grid[row][col] = true;
				}
			}
		}
		return grid;
	}

	public static boolean[][] withCenterDot(boolean[][] grid) {
		boolean[][] copy = copy(grid);
		copy[CENTER][CENTER] = true;
		return copy;
	}

	/**
	 * `centerX`/`centerY` is where the middle of the center cell lands, not that cell's top left
	 * corner - without the half-pixel correction the whole grid would sit slightly too far right and
	 * down.
	 */
	public static void render(boolean[][] grid, int centerX, int centerY, int pixelSize, int color) {
		int offset = pixelSize / 2;
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (!grid[row][col]) {
					continue;
				}
				int pixelX = centerX + (col - CENTER) * pixelSize - offset;
				int pixelY = centerY + (row - CENTER) * pixelSize - offset;
				GuiComponent.fill(pixelX, pixelY, pixelX + pixelSize, pixelY + pixelSize, color);
			}
		}
	}
}
