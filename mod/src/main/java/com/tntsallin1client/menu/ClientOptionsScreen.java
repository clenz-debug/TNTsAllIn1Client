package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Phase 5e: detailed per-feature settings, one level below {@link ClientMenuScreen}.
 * Currently just the material counter's item source, but this is where future
 * per-feature detail settings should be added rather than back in the top menu.
 */
public class ClientOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public ClientOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		EditBox itemIdBox = new EditBox(this.font, x, y + ROW_SPACING, ROW_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.menu.material_counter_item"));
		itemIdBox.setMaxLength(64);
		itemIdBox.setValue(config.materialCounterItemId);
		itemIdBox.setEditable(!config.materialCounterUseHeldItem);
		itemIdBox.active = !config.materialCounterUseHeldItem;
		itemIdBox.setResponder(value -> {
			config.materialCounterItemId = value;
			config.save();
		});

		this.addRenderableWidget(CycleButton.booleanBuilder(
						Component.translatable("gui.tntsallin1client.options.material_counter_source.held"),
						Component.translatable("gui.tntsallin1client.options.material_counter_source.fixed"),
						config.materialCounterUseHeldItem)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.options.material_counter_source"),
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
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
