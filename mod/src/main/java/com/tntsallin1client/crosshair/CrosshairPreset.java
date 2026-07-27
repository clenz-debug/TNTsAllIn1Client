package com.tntsallin1client.crosshair;

import net.minecraft.network.chat.Component;

/**
 * Fixed library of crosshair shapes, inspired by the preset list in Dawn
 * Client's crosshair picker (user-provided reference screenshot) - not a
 * pixel-perfect copy of those icons (not feasible to reproduce exactly from
 * a small screenshot, and not the point - the point was having a varied,
 * clearly distinguishable set of shapes to pick from, not matching another
 * client's art asset for asset's sake). "Heart" was intentionally left out
 * per explicit request. Each shape is a {@link CrosshairGrid#SIZE}x{@code SIZE}
 * boolean grid, same representation {@link CrosshairMode#CUSTOM} user-drawn
 * crosshairs use.
 */
public enum CrosshairPreset {
	NONE("gui.tntsallin1client.crosshair_preset.none") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.empty();
		}
	},
	SMALLER("gui.tntsallin1client.crosshair_preset.smaller") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			int c = CrosshairGrid.CENTER;
			grid[c][c - 1] = true;
			grid[c][c] = true;
			grid[c][c + 1] = true;
			grid[c - 1][c] = true;
			grid[c + 1][c] = true;
			return grid;
		}
	},
	DOT("gui.tntsallin1client.crosshair_preset.dot") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			grid[CrosshairGrid.CENTER][CrosshairGrid.CENTER] = true;
			return grid;
		}
	},
	PLUS_DOT("gui.tntsallin1client.crosshair_preset.plus_dot") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			int c = CrosshairGrid.CENTER;
			grid[c][c - 3] = true;
			grid[c][c - 2] = true;
			grid[c][c + 2] = true;
			grid[c][c + 3] = true;
			grid[c - 3][c] = true;
			grid[c - 2][c] = true;
			grid[c + 2][c] = true;
			grid[c + 3][c] = true;
			grid[c][c] = true;
			return grid;
		}
	},
	CIRCLE("gui.tntsallin1client.crosshair_preset.circle") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.circleRing(4.0F);
		}
	},
	CIRCLE_DOT("gui.tntsallin1client.crosshair_preset.circle_dot") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(CrosshairGrid.circleRing(4.0F));
		}
	},
	SQUARE("gui.tntsallin1client.crosshair_preset.square") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.squareRing(3);
		}
	},
	SQUARE_DOT("gui.tntsallin1client.crosshair_preset.square_dot") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(CrosshairGrid.squareRing(3));
		}
	},
	SQUARE_PLUS("gui.tntsallin1client.crosshair_preset.square_plus") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.squareRing(3);
			int c = CrosshairGrid.CENTER;
			grid[c][0] = true;
			grid[c][1] = true;
			grid[c][7] = true;
			grid[c][8] = true;
			grid[0][c] = true;
			grid[1][c] = true;
			grid[7][c] = true;
			grid[8][c] = true;
			return grid;
		}
	},
	SQUARE_PLUS_DOT("gui.tntsallin1client.crosshair_preset.square_plus_dot") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(SQUARE_PLUS.grid());
		}
	},
	FOUR_ANGLED("gui.tntsallin1client.crosshair_preset.four_angled") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			grid[0][0] = true;
			grid[1][1] = true;
			grid[0][8] = true;
			grid[1][7] = true;
			grid[8][0] = true;
			grid[7][1] = true;
			grid[8][8] = true;
			grid[7][7] = true;
			return grid;
		}
	},
	FOUR_ANGLED_DOT("gui.tntsallin1client.crosshair_preset.four_angled_dot") {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(FOUR_ANGLED.grid());
		}
	},
	ARROW("gui.tntsallin1client.crosshair_preset.arrow") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			grid[3][4] = true;
			grid[4][3] = true;
			grid[4][5] = true;
			grid[5][2] = true;
			grid[5][6] = true;
			return grid;
		}
	};

	private final String translationKey;

	CrosshairPreset(String translationKey) {
		this.translationKey = translationKey;
	}

	public abstract boolean[][] grid();

	public Component label() {
		return Component.translatable(this.translationKey);
	}
}
