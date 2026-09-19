package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.ArmorStatusColorMode;
import com.tntsallin1client.hud.ArmorStatusDirection;
import com.tntsallin1client.hud.ArmorStatusHud;
import com.tntsallin1client.hud.ArmorStatusIconPosition;
import com.tntsallin1client.hud.ArmorStatusLayoutMode;
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
 * Main options screen for the Armor & Tool Status display: name/icon toggles,
 * the number color (fixed, or a durability gradient for damageable items -
 * stackable items always use the fixed color regardless, see
 * {@link com.tntsallin1client.hud.ArmorStatusHud}), and the layout (six
 * individually draggable elements, or bundled horizontally/vertically into
 * one). Per-slot on/off lives in its own {@link ArmorStatusSlotsOptionsScreen},
 * reached via a button here - same "each feature/sub-area gets its own
 * focused screen" convention as {@link KeystrokesOptionsScreen}/
 * {@link KeystrokesKeysOptionsScreen}. Same scrolling shape as
 * {@link KeystrokesOptionsScreen} - a color picker plus five more rows
 * doesn't fit without it.
 */
public class ArmorStatusOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int LABEL_HEIGHT = 12;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private @Nullable ColorPickerPanel colorPicker;
	private @Nullable ScrollBarHelper scrollBar;
	private int labelX;
	private int colorLabelY;
	private int scrollOffset;
	private int maxScroll;

	public ArmorStatusOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.armor_status_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();

		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		int contentHeight = computeContentHeight(config) - TOP_MARGIN;
		this.maxScroll = Math.max(0, contentHeight - viewportHeight);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

		this.labelX = (this.width - ROW_WIDTH) / 2;
		int x = this.labelX;
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

		this.addRenderableWidget(CycleButton.onOffBuilder(config.armorStatusEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.enabled"),
						(button, value) -> {
							config.armorStatusEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.armorStatusShowName)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.show_name"),
						(button, value) -> {
							config.armorStatusShowName = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.armorStatusShowIcon)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.show_icon"),
						(button, value) -> {
							config.armorStatusShowIcon = value;
							config.save();
							this.rebuild();
						}));
		y += ROW_SPACING;

		if (config.armorStatusShowIcon) {
			this.addRenderableWidget(CycleButton.builder(
							(ArmorStatusIconPosition pos) -> Component.translatable("gui.tntsallin1client.armor_status_options.icon_position." + pos.name().toLowerCase(Locale.ROOT)),
							config.armorStatusIconPosition)
					.withValues(ArmorStatusIconPosition.values())
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.icon_position"),
							(button, pos) -> {
								config.armorStatusIconPosition = pos;
								config.save();
							}));
			y += ROW_SPACING;
		}

		this.addRenderableWidget(CycleButton.onOffBuilder(config.armorStatusShowMaxDurability)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.show_max_durability"),
						(button, value) -> {
							config.armorStatusShowMaxDurability = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.builder(
						(ArmorStatusColorMode mode) -> Component.translatable("gui.tntsallin1client.armor_status_options.color_mode." + mode.name().toLowerCase(Locale.ROOT)),
						config.armorStatusColorMode)
				.withValues(ArmorStatusColorMode.values())
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.color_mode"),
						(button, mode) -> {
							config.armorStatusColorMode = mode;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.colorLabelY = y;
		y += LABEL_HEIGHT;
		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.armorStatusColor,
				this::addRenderableWidget,
				argb -> {
					config.armorStatusColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 10;

		this.addRenderableWidget(CycleButton.builder(
						(ArmorStatusLayoutMode mode) -> Component.translatable("gui.tntsallin1client.armor_status_options.layout_mode." + mode.name().toLowerCase(Locale.ROOT)),
						config.armorStatusLayoutMode)
				.withValues(ArmorStatusLayoutMode.values())
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.layout_mode"),
						(button, mode) -> {
							config.armorStatusLayoutMode = mode;
							config.save();
							this.rebuild();
						}));
		y += ROW_SPACING;

		if (config.armorStatusLayoutMode == ArmorStatusLayoutMode.BUNDLED) {
			this.addRenderableWidget(CycleButton.builder(
							(ArmorStatusDirection direction) -> Component.translatable("gui.tntsallin1client.armor_status_options.direction." + direction.name().toLowerCase(Locale.ROOT)),
							config.armorStatusBundledDirection)
					.withValues(ArmorStatusDirection.values())
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.direction"),
							(button, direction) -> {
								config.armorStatusBundledDirection = direction;
								config.save();
							}));
			y += ROW_SPACING;

			this.addRenderableWidget(CycleButton.onOffBuilder(config.armorStatusBundledReversed)
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.armor_status_options.reversed"),
							(button, value) -> {
								ArmorStatusHud.adjustBundledPositionForReversedToggle(config, this.font, value);
								config.armorStatusBundledReversed = value;
								config.save();
							}));
			y += ROW_SPACING;
		}

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.armor_status_options.slots_button"),
						button -> this.minecraft.setScreen(new ArmorStatusSlotsOptionsScreen(this)))
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_HEIGHT + 4;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Mirrors the y-cursor arithmetic in {@link #init} - keep in sync if that layout ever changes. */
	private static int computeContentHeight(ClientConfig config) {
		int y = TOP_MARGIN;
		y += ROW_SPACING; // enabled
		y += ROW_SPACING; // show name
		y += ROW_SPACING; // show icon
		if (config.armorStatusShowIcon) {
			y += ROW_SPACING; // icon position
		}
		y += ROW_SPACING; // show max durability
		y += ROW_SPACING + 6; // color mode
		y += LABEL_HEIGHT;
		y += ColorPickerPanel.totalHeight() + 10;
		y += ROW_SPACING; // layout mode
		if (config.armorStatusLayoutMode == ArmorStatusLayoutMode.BUNDLED) {
			y += ROW_SPACING; // direction
			y += ROW_SPACING; // reversed growth
		}
		y += ROW_HEIGHT + 4; // slots button
		y += ROW_HEIGHT; // back
		return y;
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	/** Same scissor fix as {@link CrosshairOptionsScreen#render} - without it, scrolling pushes content up into the fixed title text instead of disappearing off the top. */
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.armor_status_options.color_label"),
				this.labelX, this.colorLabelY, 0xFFFFFFFF);
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
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean releasedColor = this.colorPicker.mouseReleased();
		if (this.scrollBar.mouseReleased() || releasedColor) {
			return true;
		}
		return super.mouseReleased(event);
	}

	/** Same wheel-precedence reasoning as {@link CrosshairOptionsScreen#mouseScrolled}. */
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
