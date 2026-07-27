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

	public static void render(GuiGraphics guiGraphics, boolean[][] grid, int centerX, int centerY, int pixelSize, int color) {
		int half = SIZE / 2;
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (!grid[row][col]) {
					continue;
				}
				int pixelX = centerX + (col - half) * pixelSize;
				int pixelY = centerY + (row - half) * pixelSize;
				guiGraphics.fill(pixelX, pixelY, pixelX + pixelSize, pixelY + pixelSize, color);
			}
		}
	}
}
