package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Phase 5l: dedicated options screen for the F3+B hitbox color - same hex
 * field + swatch shape as {@link CrosshairOptionsScreen}, via
 * {@link ColorPickerHelper}.
 */
public class HitboxColorOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int SWATCH_SIZE = ROW_HEIGHT;
	private static final int SWATCH_GAP = 6;
	private static final int EDIT_BOX_WIDTH = ROW_WIDTH - SWATCH_SIZE - SWATCH_GAP;

	private final Screen parent;

	public HitboxColorOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.hitbox_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		EditBox colorBox = new EditBox(this.font, x, y, EDIT_BOX_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.hitbox_options.color"));
		colorBox.setMaxLength(6);
		colorBox.setFilter(value -> value.matches("[0-9a-fA-F]{0,6}"));
		colorBox.setValue(ColorPickerHelper.toHexRgb(config.customHitboxColor));
		colorBox.setResponder(value -> {
			Integer parsed = ColorPickerHelper.parseHexRgbToArgb(value);
			if (parsed != null) {
				config.customHitboxColor = parsed;
				config.save();
			}
		});
		this.addRenderableWidget(colorBox);
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		ClientConfig config = ClientConfig.get();
		int swatchX = (this.width - ROW_WIDTH) / 2 + EDIT_BOX_WIDTH + SWATCH_GAP;
		int swatchY = 40;
		ColorPickerHelper.drawSwatch(guiGraphics, swatchX, swatchY, SWATCH_SIZE, config.customHitboxColor);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
