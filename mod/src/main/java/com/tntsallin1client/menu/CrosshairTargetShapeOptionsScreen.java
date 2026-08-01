package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.crosshair.CrosshairGrid;
import com.tntsallin1client.crosshair.CrosshairMode;
import com.tntsallin1client.crosshair.CrosshairPreset;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Phase 5ac: sibling to {@link CrosshairTargetColorOptionsScreen} - a second,
 * independently toggleable crosshair shape (same preset-library-or-custom-9x9-grid
 * representation as the base crosshair, see {@link CrosshairOptionsScreen})
 * while aiming at an attackable mob. Kept as its own screen off
 * {@link CrosshairOptionsScreen} rather than folded into the target-color
 * screen - each is independently toggleable, and the mode/preset/grid editor
 * alone is already as much content as the color picker is.
 *
 * <p>Same {@code scrollOffset}/{@code maxScroll}/{@link ScrollBarHelper}/
 * {@code rebuild()}-on-mode-change shape as {@link CrosshairOptionsScreen},
 * trimmed to just the enabled toggle, mode switch, and preset-or-grid editor -
 * pixel size and "ignore GUI Scale" are intentionally not duplicated here,
 * they stay shared with the base crosshair (see {@code CustomCrosshair}).
 */
public class CrosshairTargetShapeOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int PREVIEW_PIXEL_SIZE = 5;
	private static final int PREVIEW_AREA_SIZE = CrosshairGrid.SIZE * PREVIEW_PIXEL_SIZE;
	private static final int GRID_CELL_SIZE = 15;
	private static final int GRID_AREA_SIZE = CrosshairGrid.SIZE * GRID_CELL_SIZE;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private ScrollBarHelper scrollBar;
	private int gridX;
	private int gridY;
	private int previewX;
	private int previewY;
	private boolean painting;
	private boolean paintingValue;
	private int scrollOffset;
	private int maxScroll;

	public CrosshairTargetShapeOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.crosshair_target_shape_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();

		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		int contentHeight = computeContentHeight(config) - TOP_MARGIN;
		this.maxScroll = Math.max(0, contentHeight - viewportHeight);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

		int x = (this.width - ROW_WIDTH) / 2;
		int y = TOP_MARGIN - this.scrollOffset;

		if (this.scrollBar == null) {
			this.scrollBar = new ScrollBarHelper(x + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight,
					() -> this.scrollOffset, () -> this.maxScroll,
					newOffset -> {
						this.scrollOffset = newOffset;
						this.rebuild();
					});
		} else {
			this.scrollBar.reposition(x + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight);
		}

		this.addRenderableWidget(CycleButton.onOffBuilder(config.crosshairTargetShapeEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.crosshair_target_shape_options.enabled"),
						(button, value) -> {
							config.crosshairTargetShapeEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.builder(
						(CrosshairMode mode) -> Component.translatable("gui.tntsallin1client.crosshair_options.mode." + mode.name().toLowerCase(Locale.ROOT)),
						config.crosshairTargetShapeMode)
				.withValues(CrosshairMode.values())
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.crosshair_options.mode"),
						(button, mode) -> {
							config.crosshairTargetShapeMode = mode;
							config.save();
							this.rebuild();
						}));
		y += ROW_SPACING;

		if (config.crosshairTargetShapeMode == CrosshairMode.PRESET) {
			this.addRenderableWidget(CycleButton.builder(CrosshairPreset::label, config.crosshairTargetShapePreset)
					.withValues(CrosshairPreset.values())
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.crosshair_options.preset"),
							(button, preset) -> {
								config.crosshairTargetShapePreset = preset;
								config.save();
							}));
			y += ROW_SPACING;
			this.previewX = x + (ROW_WIDTH - PREVIEW_AREA_SIZE) / 2;
			this.previewY = y;
			y += PREVIEW_AREA_SIZE + 6;
		} else {
			this.gridX = x + (ROW_WIDTH - GRID_AREA_SIZE) / 2;
			this.gridY = y;
			y += GRID_AREA_SIZE + 6;
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Mirrors {@link #init}'s y-cursor arithmetic - keep in sync if that layout ever changes. */
	private static int computeContentHeight(ClientConfig config) {
		int y = TOP_MARGIN;
		y += ROW_SPACING;
		y += ROW_SPACING;
		if (config.crosshairTargetShapeMode == CrosshairMode.PRESET) {
			y += ROW_SPACING;
			y += PREVIEW_AREA_SIZE + 6;
		} else {
			y += GRID_AREA_SIZE + 6;
		}
		y += ROW_HEIGHT;
		return y;
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		ClientConfig config = ClientConfig.get();
		if (config.crosshairTargetShapeMode == CrosshairMode.PRESET) {
			guiGraphics.fill(this.previewX - 2, this.previewY - 2, this.previewX + PREVIEW_AREA_SIZE + 2, this.previewY + PREVIEW_AREA_SIZE + 2, 0x80000000);
			CrosshairGrid.render(guiGraphics, config.crosshairTargetShapePreset.grid(),
					this.previewX + PREVIEW_AREA_SIZE / 2, this.previewY + PREVIEW_AREA_SIZE / 2, PREVIEW_PIXEL_SIZE, config.customCrosshairColor);
		} else {
			guiGraphics.fill(this.gridX, this.gridY, this.gridX + GRID_AREA_SIZE, this.gridY + GRID_AREA_SIZE, 0x80000000);
			for (int row = 0; row < CrosshairGrid.SIZE; row++) {
				for (int col = 0; col < CrosshairGrid.SIZE; col++) {
					int cellX = this.gridX + col * GRID_CELL_SIZE;
					int cellY = this.gridY + row * GRID_CELL_SIZE;
					boolean on = config.crosshairTargetShapeCustomGrid[row][col];
					guiGraphics.fill(cellX, cellY, cellX + GRID_CELL_SIZE - 1, cellY + GRID_CELL_SIZE - 1,
							on ? config.customCrosshairColor : 0xFF3A3A3A);
				}
			}
		}
		guiGraphics.disableScissor();

		this.scrollBar.render(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.scrollBar.mouseClicked(event)) {
			return true;
		}
		if (ClientConfig.get().crosshairTargetShapeMode == CrosshairMode.CUSTOM && isInGrid(event.x(), event.y())) {
			int[] cell = cellAt(event.x(), event.y());
			ClientConfig config = ClientConfig.get();
			this.paintingValue = !config.crosshairTargetShapeCustomGrid[cell[0]][cell[1]];
			this.painting = true;
			paintCell(event.x(), event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		if (this.painting) {
			paintCell(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean wasPainting = this.painting;
		this.painting = false;
		if (this.scrollBar.mouseReleased() || wasPainting) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
			return true;
		}
		if (this.maxScroll <= 0) {
			return false;
		}
		int newOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollDeltaY * SCROLL_STEP), 0, this.maxScroll);
		if (newOffset != this.scrollOffset) {
			this.scrollOffset = newOffset;
			this.rebuild();
		}
		return true;
	}

	private boolean isInGrid(double mouseX, double mouseY) {
		return mouseX >= this.gridX && mouseX < this.gridX + GRID_AREA_SIZE
				&& mouseY >= this.gridY && mouseY < this.gridY + GRID_AREA_SIZE;
	}

	private int[] cellAt(double mouseX, double mouseY) {
		int col = Mth.clamp((int) ((mouseX - this.gridX) / GRID_CELL_SIZE), 0, CrosshairGrid.SIZE - 1);
		int row = Mth.clamp((int) ((mouseY - this.gridY) / GRID_CELL_SIZE), 0, CrosshairGrid.SIZE - 1);
		return new int[]{row, col};
	}

	private void paintCell(double mouseX, double mouseY) {
		if (!isInGrid(mouseX, mouseY)) {
			return;
		}
		int[] cell = cellAt(mouseX, mouseY);
		ClientConfig config = ClientConfig.get();
		if (config.crosshairTargetShapeCustomGrid[cell[0]][cell[1]] == this.paintingValue) {
			return;
		}
		config.crosshairTargetShapeCustomGrid[cell[0]][cell[1]] = this.paintingValue;
		config.save();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
