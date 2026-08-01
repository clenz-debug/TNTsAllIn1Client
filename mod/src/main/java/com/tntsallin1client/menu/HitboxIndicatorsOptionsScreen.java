package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5ab: per-indicator on/off + color for the four secondary F3+B
 * elements 5aa restored (vehicle mount marker, eye-height line, view-
 * direction arrow, ender dragon sub-parts) - split out from
 * {@link HitboxColorOptionsScreen} into its own screen (opened via a button
 * there) since four {@link ColorPickerPanel}s plus their toggles don't come
 * close to fitting on one screen. Reachable so a chosen main hitbox color
 * can be kept from visually blending into one of these, by moving that
 * indicator to a different color or turning it off entirely.
 *
 * <p>Same scrolling shape as {@link CrosshairOptionsScreen}
 * ({@code scrollOffset}/{@code maxScroll}, {@link ScrollBarHelper}, a
 * {@link #computeContentHeight()} mirroring {@link #init()}'s y-cursor
 * arithmetic) - four toggle+picker groups are far taller than any screen.
 */
public class HitboxIndicatorsOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int GROUP_GAP = 12;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private final List<ColorPickerPanel> colorPickers = new ArrayList<>();
	private ScrollBarHelper scrollBar;
	private int scrollOffset;
	private int maxScroll;

	public HitboxIndicatorsOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.hitbox_indicators_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		this.colorPickers.clear();

		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		int contentHeight = computeContentHeight() - TOP_MARGIN;
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

		this.addRenderableWidget(CycleButton.onOffBuilder(config.customHitboxShowEyeHeight)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.hitbox_indicators_options.eye_height"),
						(button, value) -> {
							config.customHitboxShowEyeHeight = value;
							config.save();
						}));
		y += ROW_SPACING;
		this.colorPickers.add(new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.customHitboxEyeHeightColor,
				this::addRenderableWidget,
				argb -> {
					config.customHitboxEyeHeightColor = argb;
					config.save();
				}));
		y += ColorPickerPanel.totalHeight() + GROUP_GAP;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.customHitboxShowVehicleMarker)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.hitbox_indicators_options.vehicle_marker"),
						(button, value) -> {
							config.customHitboxShowVehicleMarker = value;
							config.save();
						}));
		y += ROW_SPACING;
		this.colorPickers.add(new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.customHitboxVehicleMarkerColor,
				this::addRenderableWidget,
				argb -> {
					config.customHitboxVehicleMarkerColor = argb;
					config.save();
				}));
		y += ColorPickerPanel.totalHeight() + GROUP_GAP;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.customHitboxShowViewDirection)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.hitbox_indicators_options.view_direction"),
						(button, value) -> {
							config.customHitboxShowViewDirection = value;
							config.save();
						}));
		y += ROW_SPACING;
		this.colorPickers.add(new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.customHitboxViewDirectionColor,
				this::addRenderableWidget,
				argb -> {
					config.customHitboxViewDirectionColor = argb;
					config.save();
				}));
		y += ColorPickerPanel.totalHeight() + GROUP_GAP;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.customHitboxShowDragonParts)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.hitbox_indicators_options.dragon_parts"),
						(button, value) -> {
							config.customHitboxShowDragonParts = value;
							config.save();
						}));
		y += ROW_SPACING;
		this.colorPickers.add(new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.customHitboxDragonPartsColor,
				this::addRenderableWidget,
				argb -> {
					config.customHitboxDragonPartsColor = argb;
					config.save();
				}));
		y += ColorPickerPanel.totalHeight() + GROUP_GAP;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Mirrors {@link #init}'s y-cursor arithmetic - four identical toggle+picker groups, so a simple multiply. */
	private static int computeContentHeight() {
		int groupHeight = ROW_SPACING + ColorPickerPanel.totalHeight() + GROUP_GAP;
		return TOP_MARGIN + 4 * groupHeight + ROW_HEIGHT;
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		for (ColorPickerPanel colorPicker : this.colorPickers) {
			colorPicker.render(guiGraphics, 0xFFFFFFFF);
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
		for (ColorPickerPanel colorPicker : this.colorPickers) {
			if (colorPicker.mouseClicked(event)) {
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		for (ColorPickerPanel colorPicker : this.colorPickers) {
			if (colorPicker.mouseDragged(event)) {
				return true;
			}
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = this.scrollBar.mouseReleased();
		for (ColorPickerPanel colorPicker : this.colorPickers) {
			handled |= colorPicker.mouseReleased();
		}
		if (handled) {
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

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
