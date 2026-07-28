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
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Phase 5e/5i, redesigned: dedicated options screen for the custom crosshair.
 * Originally just a hex color field; now picks between a preset shape
 * library ({@link CrosshairPreset}) or a hand-drawn 9x9 grid, a size
 * independent of GUI Scale, and the base color via {@link ColorPickerPanel} -
 * inspired by Dawn Client's crosshair editor (user-provided reference
 * screenshot). The target-color-while-aiming-at-a-mob setting lives in its
 * own {@link CrosshairTargetColorOptionsScreen} rather than crammed onto this
 * already-tall screen, same "each feature gets a focused screen" reasoning
 * as everywhere else in this mod.
 *
 * <p>Re-runs {@link #init()} whenever the mode changes or the page is
 * scrolled ({@link #rebuild()}) instead of trying to show/hide widgets or
 * reposition them individually - the PRESET and CUSTOM sections have
 * completely different shapes/heights (a cycle button plus a small preview
 * vs. a 9x9 clickable pixel grid), and re-running the same layout code with
 * a different {@code scrollOffset} baked in is simpler and less bug-prone
 * than juggling widget visibility or calling {@code setY} on everything by
 * hand.
 *
 * <p><b>Bugfixes from the first live test:</b> the preset preview position
 * used to be recomputed from scratch in {@link #render}, independently of
 * the position actually used in {@link #init} - the two drifted apart
 * (missing the preset cycle button's own row height), so the preview drew
 * directly on top of that button. Fixed by storing {@link #previewX}/
 * {@link #previewY} as fields set once in {@code init}, the same single
 * source of truth {@link #gridX}/{@link #gridY} already used for the custom
 * grid editor - {@code render} just reads them back instead of recalculating.
 */
public class CrosshairOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int PREVIEW_PIXEL_SIZE = 5;
	private static final int PREVIEW_AREA_SIZE = CrosshairGrid.SIZE * PREVIEW_PIXEL_SIZE;
	private static final int GRID_CELL_SIZE = 15;
	private static final int GRID_AREA_SIZE = CrosshairGrid.SIZE * GRID_CELL_SIZE;
	private static final int MIN_PIXEL_SIZE = 1;
	private static final int MAX_PIXEL_SIZE = 6;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private @Nullable ColorPickerPanel colorPicker;
	private @Nullable ScrollBarHelper scrollBar;
	private int gridX;
	private int gridY;
	private int previewX;
	private int previewY;
	private boolean painting;
	private boolean paintingValue;
	private int scrollOffset;
	private int maxScroll;

	public CrosshairOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.crosshair_options.title"));
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

		this.addRenderableWidget(CycleButton.builder(
						(CrosshairMode mode) -> Component.translatable("gui.tntsallin1client.crosshair_options.mode." + mode.name().toLowerCase(Locale.ROOT)),
						config.crosshairMode)
				.withValues(CrosshairMode.values())
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.crosshair_options.mode"),
						(button, mode) -> {
							config.crosshairMode = mode;
							config.save();
							this.rebuild();
						}));
		y += ROW_SPACING;

		if (config.crosshairMode == CrosshairMode.PRESET) {
			this.addRenderableWidget(CycleButton.builder(CrosshairPreset::label, config.crosshairPreset)
					.withValues(CrosshairPreset.values())
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.crosshair_options.preset"),
							(button, preset) -> {
								config.crosshairPreset = preset;
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

		int sizeSliderWidth = 100;
		this.addRenderableWidget(new IntSliderButton(x, y, sizeSliderWidth, ROW_HEIGHT, MIN_PIXEL_SIZE, MAX_PIXEL_SIZE, config.crosshairPixelSize,
				size -> Component.translatable("gui.tntsallin1client.crosshair_options.pixel_size", size),
				size -> {
					config.crosshairPixelSize = size;
					config.save();
				}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.crosshairIgnoreGuiScale)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.crosshair_options.ignore_gui_scale"),
						(button, value) -> {
							config.crosshairIgnoreGuiScale = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.customCrosshairColor,
				this::addRenderableWidget,
				argb -> {
					config.customCrosshairColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 6;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.crosshair_options.target_color_button"),
						button -> this.minecraft.setScreen(new CrosshairTargetColorOptionsScreen(this)))
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/**
	 * Mirrors the y-cursor arithmetic in {@link #init} to get the total
	 * unscrolled content height without actually creating widgets - needed to
	 * clamp {@link #scrollOffset} before laying anything out. Keep in sync
	 * with {@link #init} if that layout ever changes.
	 */
	private static int computeContentHeight(ClientConfig config) {
		int y = TOP_MARGIN;
		y += ROW_SPACING;
		if (config.crosshairMode == CrosshairMode.PRESET) {
			y += ROW_SPACING;
			y += PREVIEW_AREA_SIZE + 6;
		} else {
			y += GRID_AREA_SIZE + 6;
		}
		y += ROW_SPACING;
		y += ROW_SPACING + 6;
		y += ColorPickerPanel.totalHeight() + 6;
		y += ROW_SPACING;
		y += ROW_HEIGHT;
		return y;
	}

	/** Full re-layout after the mode toggle or scroll offset changes what's shown/where. */
	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	/**
	 * Scrolled rows are just shifted by {@code -scrollOffset} (see {@link #init}),
	 * nothing clips them - without this scissor, scrolling far enough pushes a
	 * row up into the fixed title text at y=12 instead of disappearing off the
	 * top like a real scrollable list would (vanilla's {@link ContainerObjectSelectionList},
	 * used elsewhere in this mod, clips for exactly this reason). Bounds match
	 * the same [TOP_MARGIN, height - BOTTOM_MARGIN] viewport already used to
	 * compute {@link #maxScroll}.
	 */
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		ClientConfig config = ClientConfig.get();
		if (config.crosshairMode == CrosshairMode.PRESET) {
			guiGraphics.fill(this.previewX - 2, this.previewY - 2, this.previewX + PREVIEW_AREA_SIZE + 2, this.previewY + PREVIEW_AREA_SIZE + 2, 0x80000000);
			CrosshairGrid.render(guiGraphics, config.crosshairPreset.grid(),
					this.previewX + PREVIEW_AREA_SIZE / 2, this.previewY + PREVIEW_AREA_SIZE / 2, PREVIEW_PIXEL_SIZE, config.customCrosshairColor);
		} else {
			guiGraphics.fill(this.gridX, this.gridY, this.gridX + GRID_AREA_SIZE, this.gridY + GRID_AREA_SIZE, 0x80000000);
			for (int row = 0; row < CrosshairGrid.SIZE; row++) {
				for (int col = 0; col < CrosshairGrid.SIZE; col++) {
					int cellX = this.gridX + col * GRID_CELL_SIZE;
					int cellY = this.gridY + row * GRID_CELL_SIZE;
					boolean on = config.crosshairCustomGrid[row][col];
					guiGraphics.fill(cellX, cellY, cellX + GRID_CELL_SIZE - 1, cellY + GRID_CELL_SIZE - 1,
							on ? config.customCrosshairColor : 0xFF3A3A3A);
				}
			}
		}

		this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
		guiGraphics.disableScissor();

		this.scrollBar.render(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.scrollBar.mouseClicked(event)) {
			return true;
		}
		if (this.colorPicker.mouseClicked(event)) {
			return true;
		}
		if (ClientConfig.get().crosshairMode == CrosshairMode.CUSTOM && isInGrid(event.x(), event.y())) {
			int[] cell = cellAt(event.x(), event.y());
			ClientConfig config = ClientConfig.get();
			this.paintingValue = !config.crosshairCustomGrid[cell[0]][cell[1]];
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
		if (this.colorPicker.mouseDragged(event)) {
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
		if (this.scrollBar.mouseReleased() || this.colorPicker.mouseReleased() || wasPainting) {
			return true;
		}
		return super.mouseReleased(event);
	}

	/**
	 * Lets widgets under the cursor (e.g. the mode/preset {@code CycleButton}s,
	 * which already cycle their own value on scroll) handle the wheel first;
	 * only scrolls the page if nothing consumed it, so hovering a cycle button
	 * and scrolling still changes that button's value like anywhere else in
	 * vanilla, instead of always scrolling the screen out from under it.
	 */
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

	/** Applies {@link #paintingValue} (decided once, from the cell the paint stroke started on) to whatever cell the mouse is over now. */
	private void paintCell(double mouseX, double mouseY) {
		if (!isInGrid(mouseX, mouseY)) {
			return;
		}
		int[] cell = cellAt(mouseX, mouseY);
		ClientConfig config = ClientConfig.get();
		if (config.crosshairCustomGrid[cell[0]][cell[1]] == this.paintingValue) {
			return;
		}
		config.crosshairCustomGrid[cell[0]][cell[1]] = this.paintingValue;
		config.save();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
