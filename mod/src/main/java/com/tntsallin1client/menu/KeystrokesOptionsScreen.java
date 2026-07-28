package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5m, extended Phase 5t: dedicated options screen for the keystrokes
 * overlay - the active-box (pressed) color and the key-label text color
 * (both via the shared {@link ColorPickerPanel}), plus a button into
 * {@link KeystrokesKeysOptionsScreen} for showing/hiding individual keys.
 * The idle box color is not exposed - only the "lights up" color was asked
 * for. Two full color pickers plus the keys/back buttons no longer fit
 * without scrolling, so this uses the same scroll pattern as
 * {@link CrosshairOptionsScreen}.
 */
public class KeystrokesOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int LABEL_HEIGHT = 12;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private @Nullable ColorPickerPanel activeColorPicker;
	private @Nullable ColorPickerPanel textColorPicker;
	private @Nullable ScrollBarHelper scrollBar;
	private int labelX;
	private int activeLabelY;
	private int textLabelY;
	private int scrollOffset;
	private int maxScroll;

	public KeystrokesOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.keystrokes_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();

		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		int contentHeight = computeContentHeight() - TOP_MARGIN;
		this.maxScroll = Math.max(0, contentHeight - viewportHeight);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

		this.labelX = (this.width - ROW_WIDTH) / 2;
		int y = TOP_MARGIN - this.scrollOffset;

		this.scrollBar = new ScrollBarHelper(this.labelX + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight,
				() -> this.scrollOffset, () -> this.maxScroll,
				newOffset -> {
					this.scrollOffset = newOffset;
					this.rebuild();
				});

		this.activeLabelY = y;
		y += LABEL_HEIGHT;
		this.activeColorPicker = new ColorPickerPanel(this.font, this.labelX, y, ROW_WIDTH, config.keystrokesActiveColor,
				this::addRenderableWidget,
				argb -> {
					config.keystrokesActiveColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 10;

		this.textLabelY = y;
		y += LABEL_HEIGHT;
		this.textColorPicker = new ColorPickerPanel(this.font, this.labelX, y, ROW_WIDTH, config.keystrokesTextColor,
				this::addRenderableWidget,
				argb -> {
					config.keystrokesTextColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 10;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.keystrokes_options.keys_button"),
						button -> this.minecraft.setScreen(new KeystrokesKeysOptionsScreen(this)))
				.bounds(this.labelX, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_HEIGHT + 4;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(this.labelX, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Mirrors the y-cursor arithmetic in {@link #init} - keep in sync if that layout ever changes. */
	private static int computeContentHeight() {
		int y = TOP_MARGIN;
		y += LABEL_HEIGHT;
		y += ColorPickerPanel.totalHeight() + 10;
		y += LABEL_HEIGHT;
		y += ColorPickerPanel.totalHeight() + 10;
		y += ROW_HEIGHT + 4;
		y += ROW_HEIGHT;
		return y;
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	/**
	 * Same scissor fix as {@link CrosshairOptionsScreen#render} - without it,
	 * scrolling pushes the active-color label/picker up into the fixed title
	 * text instead of disappearing off the top.
	 */
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.keystrokes_options.active_color_label"),
				this.labelX, this.activeLabelY, 0xFFFFFFFF);
		this.activeColorPicker.render(guiGraphics, 0xFFFFFFFF);
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.keystrokes_options.text_color_label"),
				this.labelX, this.textLabelY, 0xFFFFFFFF);
		this.textColorPicker.render(guiGraphics, 0xFFFFFFFF);
		guiGraphics.disableScissor();

		this.scrollBar.render(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.scrollBar.mouseClicked(event)) {
			return true;
		}
		if (this.activeColorPicker.mouseClicked(event) || this.textColorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		if (this.activeColorPicker.mouseDragged(event) || this.textColorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean releasedActive = this.activeColorPicker.mouseReleased();
		boolean releasedText = this.textColorPicker.mouseReleased();
		if (this.scrollBar.mouseReleased() || releasedActive || releasedText) {
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
