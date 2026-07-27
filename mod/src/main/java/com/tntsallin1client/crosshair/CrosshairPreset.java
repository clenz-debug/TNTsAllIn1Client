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
	// The plus arms run the full width/height of the grid, straight through
	// the square ring rather than stopping at its outside edge (bugfix -
	// originally only drew short ticks outside the box, per user feedback:
	// "die Arme sollen durch die Box durchgehen").
	SQUARE_PLUS("gui.tntsallin1client.crosshair_preset.square_plus") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.squareRing(3);
			int c = CrosshairGrid.CENTER;
			for (int i = 0; i < CrosshairGrid.SIZE; i++) {
				grid[c][i] = true;
				grid[i][c] = true;
			}
			return grid;
		}
	},
	// Same through-the-box arms as SQUARE_PLUS, but with a gap around the
	// center dot (same gap width as PLUS_DOT) instead of one continuous line -
	// otherwise the dot wouldn't stand out from the arms at all.
	SQUARE_PLUS_DOT("gui.tntsallin1client.crosshair_preset.square_plus_dot") {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.squareRing(3);
			int c = CrosshairGrid.CENTER;
			for (int i = 0; i < CrosshairGrid.SIZE; i++) {
				if (i <= c - 2 || i >= c + 2) {
					grid[c][i] = true;
					grid[i][c] = true;
				}
			}
			grid[c][c] = true;
			return grid;
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
