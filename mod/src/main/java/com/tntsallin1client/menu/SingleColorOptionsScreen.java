package com.tntsallin1client.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * Phase 5t: generic single-color editor, reused by every row in
 * {@link ColorMenuScreen} instead of a near-identical screen subclass per
 * color (the previous shape of {@code CrosshairTargetColorOptionsScreen},
 * {@code HitboxColorOptionsScreen}, {@code BlockOutlineColorOptionsScreen}
 * and {@code KeystrokesOptionsScreen}'s color half, all now removed) - each
 * of those wrapped the exact same {@link ColorPickerPanel} with nothing else
 * feature-specific left once the color moved out of the feature's own
 * options screen.
 */
public class SingleColorOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;

	private final Screen parent;
	private final int initialColor;
	private final IntConsumer onChange;
	private @Nullable ColorPickerPanel colorPicker;

	public SingleColorOptionsScreen(Screen parent, Component title, int initialColor, IntConsumer onChange) {
		super(title);
		this.parent = parent;
		this.initialColor = initialColor;
		this.onChange = onChange;
	}

	@Override
	protected void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, this.initialColor,
				this::addRenderableWidget, this.onChange);
		y += ColorPickerPanel.totalHeight() + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.colorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.colorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.colorPicker.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
