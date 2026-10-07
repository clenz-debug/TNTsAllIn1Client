package com.tntsallin1client.menu;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CrosshairGrid;
import com.tntsallin1client.crosshair.CrosshairMode;
import com.tntsallin1client.crosshair.CrosshairPreset;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

/**
 * Base of the screens on which a crosshair shape is chosen - the crosshair itself and the one shown
 * while aiming at a mob: a switch between the preset library and drawing one, then either the
 * preset with a preview of it or the grid to draw on.
 */
abstract class CrosshairShapeScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.crosshair_options.";

	protected CrosshairShapeScreen(Screen parent, String titleKey) {
		super(parent, titleKey);
	}

	/** `grid` is the grid the player draws on, changed in place. */
	protected final void addShapeOptions(final Supplier<CrosshairMode> mode, final Consumer<CrosshairMode> setMode,
			final Supplier<CrosshairPreset> preset, final Consumer<CrosshairPreset> setPreset, final Supplier<boolean[][]> grid) {
		addChoice(KEY + "mode", KEY + "mode.preset", KEY + "mode.custom",
				() -> mode.get() == CrosshairMode.PRESET,
				usePreset -> setMode.accept(usePreset ? CrosshairMode.PRESET : CrosshairMode.CUSTOM));
		addCycle(KEY + "preset", () -> preset.get().labelKey(), () -> setPreset.accept(preset.get().next()))
				.onlyIf(() -> mode.get() == CrosshairMode.PRESET);
		addPanel(new PreviewPanel(() -> preset.get().grid(), () -> ClientConfig.get().customCrosshairColor))
				.onlyIf(() -> mode.get() == CrosshairMode.PRESET);
		addPanel(new PaintPanel(grid))
				.onlyIf(() -> mode.get() == CrosshairMode.CUSTOM);
	}

	/** Shows a shape enlarged, in the crosshair's color. */
	private static final class PreviewPanel implements OptionPanel {
		private static final int PIXEL_SIZE = 5;
		private static final int AREA_SIZE = CrosshairGrid.SIZE * PIXEL_SIZE;
		private static final int BORDER = 2;

		private final Supplier<boolean[][]> grid;
		private final IntSupplier color;

		PreviewPanel(Supplier<boolean[][]> grid, IntSupplier color) {
			this.grid = grid;
			this.color = color;
		}

		@Override
		public int height() {
			return AREA_SIZE + 2 * BORDER;
		}

		@Override
		public void render(int x, int y, int width, int mouseX, int mouseY) {
			int left = x + (width - AREA_SIZE) / 2;
			int top = y + BORDER;
			GuiComponent.fill(left - BORDER, top - BORDER, left + AREA_SIZE + BORDER, top + AREA_SIZE + BORDER, 0x80000000);
			CrosshairGrid.render(this.grid.get(), left + AREA_SIZE / 2, top + AREA_SIZE / 2, PIXEL_SIZE, this.color.getAsInt());
		}
	}

	/**
	 * The grid to draw a shape on: a click switches a cell, dragging on paints every cell passed with
	 * what the first one became.
	 */
	private static final class PaintPanel implements OptionPanel {
		private static final int CELL_SIZE = 15;
		private static final int AREA_SIZE = CrosshairGrid.SIZE * CELL_SIZE;
		// Set cells are always white, whatever the crosshair's color - a color that reads fine against
		// the sky can be hard to tell from the dark unset cells.
		private static final int ON_COLOR = 0xFFFFFFFF;
		private static final int OFF_COLOR = 0xFF3A3A3A;

		private final Supplier<boolean[][]> grid;
		private boolean painting;
		private boolean paintingValue;

		PaintPanel(Supplier<boolean[][]> grid) {
			this.grid = grid;
		}

		@Override
		public int height() {
			return AREA_SIZE;
		}

		@Override
		public void render(int x, int y, int width, int mouseX, int mouseY) {
			int left = left(x, width);
			boolean[][] cells = this.grid.get();
			GuiComponent.fill(left, y, left + AREA_SIZE, y + AREA_SIZE, 0x80000000);
			for (int row = 0; row < CrosshairGrid.SIZE; row++) {
				for (int col = 0; col < CrosshairGrid.SIZE; col++) {
					int cellX = left + col * CELL_SIZE;
					int cellY = y + row * CELL_SIZE;
					GuiComponent.fill(cellX, cellY, cellX + CELL_SIZE - 1, cellY + CELL_SIZE - 1, cells[row][col] ? ON_COLOR : OFF_COLOR);
				}
			}
		}

		@Override
		public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
			if (!isInGrid(x, y, width, mouseX, mouseY)) {
				return false;
			}
			this.paintingValue = !this.grid.get()[row(y, mouseY)][col(x, width, mouseX)];
			this.painting = true;
			paint(x, y, width, mouseX, mouseY);
			return true;
		}

		@Override
		public void mouseDragged(int x, int y, int width, int mouseX, int mouseY) {
			if (this.painting) {
				paint(x, y, width, mouseX, mouseY);
			}
		}

		@Override
		public void mouseReleased() {
			this.painting = false;
		}

		private void paint(int x, int y, int width, int mouseX, int mouseY) {
			if (!isInGrid(x, y, width, mouseX, mouseY)) {
				return;
			}
			boolean[][] cells = this.grid.get();
			int row = row(y, mouseY);
			int col = col(x, width, mouseX);
			if (cells[row][col] != this.paintingValue) {
				cells[row][col] = this.paintingValue;
				ClientConfig.get().save();
			}
		}

		private static int left(int x, int width) {
			return x + (width - AREA_SIZE) / 2;
		}

		private static boolean isInGrid(int x, int y, int width, int mouseX, int mouseY) {
			int left = left(x, width);
			return mouseX >= left && mouseX < left + AREA_SIZE && mouseY >= y && mouseY < y + AREA_SIZE;
		}

		private static int row(int y, int mouseY) {
			return Mth.clamp((mouseY - y) / CELL_SIZE, 0, CrosshairGrid.SIZE - 1);
		}

		private static int col(int x, int width, int mouseX) {
			return Mth.clamp((mouseX - left(x, width)) / CELL_SIZE, 0, CrosshairGrid.SIZE - 1);
		}
	}
}
