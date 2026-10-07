package com.tntsallin1client.crosshair;

import java.util.Locale;

/**
 * Fixed library of crosshair shapes - the same ones, under the same names, as in the Fabric
 * versions of the mod. Each is a {@link CrosshairGrid#SIZE} x {@code SIZE} grid, the representation
 * {@link CrosshairMode#CUSTOM} crosshairs use too.
 */
public enum CrosshairPreset {
	NONE {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.empty();
		}
	},
	/** A full-width, full-height cross like the game's own - so that shape is available in any color. */
	STANDARD {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			int c = CrosshairGrid.CENTER;
			for (int i = 0; i < CrosshairGrid.SIZE; i++) {
				grid[c][i] = true;
				grid[i][c] = true;
			}
			return grid;
		}
	},
	SMALLER {
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
	DOT {
		@Override
		public boolean[][] grid() {
			boolean[][] grid = CrosshairGrid.empty();
			grid[CrosshairGrid.CENTER][CrosshairGrid.CENTER] = true;
			return grid;
		}
	},
	PLUS_DOT {
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
	CIRCLE {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.circleRing(4.0F);
		}
	},
	CIRCLE_DOT {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(CrosshairGrid.circleRing(4.0F));
		}
	},
	SQUARE {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.squareRing(3);
		}
	},
	SQUARE_DOT {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(CrosshairGrid.squareRing(3));
		}
	},
	/** The plus arms run the full width and height of the grid, straight through the square ring. */
	SQUARE_PLUS {
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
	/** Like {@link #SQUARE_PLUS}, with a gap around the center dot so it stands out from the arms. */
	SQUARE_PLUS_DOT {
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
	FOUR_ANGLED {
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
	FOUR_ANGLED_DOT {
		@Override
		public boolean[][] grid() {
			return CrosshairGrid.withCenterDot(FOUR_ANGLED.grid());
		}
	},
	ARROW {
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

	public abstract boolean[][] grid();

	/** Translation key of the shape's name. */
	public String labelKey() {
		return "gui.tntsallin1client.crosshair_preset." + name().toLowerCase(Locale.ROOT);
	}

	/** The shape after this one, the first after the last. */
	public CrosshairPreset next() {
		CrosshairPreset[] all = values();
		return all[(ordinal() + 1) % all.length];
	}
}
