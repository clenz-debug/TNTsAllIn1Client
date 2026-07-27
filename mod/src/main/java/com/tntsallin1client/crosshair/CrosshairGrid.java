package com.tntsallin1client.crosshair;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A crosshair (preset or user-drawn) is a small square grid of on/off
 * pixels - same representation for both, so one render routine covers
 * everything. {@link #circleRing}/{@link #squareRing} generate the round
 * presets programmatically (distance-from-center thresholding) instead of
 * hand-authoring 81 booleans by eye per shape, which is both less error-prone
 * and reusable across every "ring" preset (plain, plus a dot).
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
	 * {@code (centerX, centerY)} is where the exact center of the center cell
	 * should land, not that cell's top-left corner - each cell is drawn
	 * {@code pixelSize} wide starting from its top-left, so without the
	 * {@code -pixelSize / 2} correction the whole grid would sit half a pixel
	 * too far right/down (confirmed visually: the in-game crosshair and the
	 * options-screen preset preview were both off-center from their intended
	 * anchor point before this fix).
	 *
	 * <p>{@code pixelSize} is a {@code float}, not an {@code int} - crosshair
	 * size now goes down to 0.25, well below one physical pixel per grid
	 * cell. Cell positions are computed in float and only rounded at the
	 * final {@code fill(...)} call; each cell still fills at least 1 physical
	 * pixel ({@code Math.max(1, Math.round(pixelSize))}) so it stays visible
	 * rather than vanishing at sub-pixel sizes - at very small sizes,
	 * adjacent grid cells naturally round to the same screen pixel and the
	 * shape compresses into a blob/dot, which is the expected trade-off of
	 * asking for a crosshair only a couple of pixels wide in the first place.
	 */
	public static void render(GuiGraphics guiGraphics, boolean[][] grid, int centerX, int centerY, float pixelSize, int color) {
		int half = SIZE / 2;
		float offset = pixelSize / 2.0F;
		int cellSize = Math.max(1, Math.round(pixelSize));
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (!grid[row][col]) {
					continue;
				}
				int pixelX = Math.round(centerX + (col - half) * pixelSize - offset);
				int pixelY = Math.round(centerY + (row - half) * pixelSize - offset);
				guiGraphics.fill(pixelX, pixelY, pixelX + cellSize, pixelY + cellSize, color);
			}
		}
	}
}
