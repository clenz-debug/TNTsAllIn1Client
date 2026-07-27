package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Phase 5e: dedicated options screen for the material counter feature only.
 * Each feature that grows beyond a simple on/off toggle gets its own screen
 * like this one, reached from {@link ClientMenuScreen}, rather than a single
 * shared options screen for every feature - keeps each one focused instead of
 * turning into one long, unrelated settings list.
 */
public class MaterialCounterOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public MaterialCounterOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.material_counter_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		EditBox itemIdBox = new EditBox(this.font, x, y + ROW_SPACING, ROW_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.material_counter_options.item_id"));
		itemIdBox.setMaxLength(64);
		itemIdBox.setValue(config.materialCounterItemId);
		itemIdBox.setEditable(!config.materialCounterUseHeldItem);
		itemIdBox.active = !config.materialCounterUseHeldItem;
		itemIdBox.setResponder(value -> {
			config.materialCounterItemId = value;
			config.save();
		});

		this.addRenderableWidget(CycleButton.booleanBuilder(
						Component.translatable("gui.tntsallin1client.material_counter_options.source.held"),
						Component.translatable("gui.tntsallin1client.material_counter_options.source.fixed"),
						config.materialCounterUseHeldItem)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.material_counter_options.source"),
						(button, value) -> {
							config.materialCounterUseHeldItem = value;
							config.save();
							itemIdBox.setEditable(!value);
							itemIdBox.active = !value;
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(itemIdBox);
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
