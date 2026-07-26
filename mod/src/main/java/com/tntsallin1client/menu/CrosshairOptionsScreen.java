package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Phase 5e/5i: dedicated options screen for the custom crosshair - just a hex
 * color field (6 hex digits, no "#") plus a small live preview swatch. Always
 * saves with full alpha (0xFF______) baked in; a bare RGB value would be
 * invisible when passed to GuiGraphics#fill, same bug class as Phase 2's
 * client-name-label alpha issue.
 */
public class CrosshairOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int SWATCH_SIZE = ROW_HEIGHT;
	private static final int SWATCH_GAP = 6;
	private static final int EDIT_BOX_WIDTH = ROW_WIDTH - SWATCH_SIZE - SWATCH_GAP;

	private final Screen parent;

	public CrosshairOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.crosshair_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		EditBox colorBox = new EditBox(this.font, x, y, EDIT_BOX_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.crosshair_options.color"));
		colorBox.setMaxLength(6);
		colorBox.setFilter(value -> value.matches("[0-9a-fA-F]{0,6}"));
		colorBox.setValue(String.format("%06X", config.customCrosshairColor & 0xFFFFFF));
		colorBox.setResponder(value -> {
			if (value.length() == 6) {
				config.customCrosshairColor = 0xFF000000 | Integer.parseInt(value, 16);
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
		guiGraphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000);
		guiGraphics.fill(swatchX + 1, swatchY + 1, swatchX + SWATCH_SIZE - 1, swatchY + SWATCH_SIZE - 1, config.customCrosshairColor);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
