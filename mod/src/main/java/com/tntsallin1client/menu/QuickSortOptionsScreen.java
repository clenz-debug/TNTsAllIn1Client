package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Phase 5z: dedicated options screen for the quick-sort feature - just the
 * sort-mode switch, same minimal shape as {@link FpsCounterOptionsScreen}.
 * Reads as a plain "Sort by Name"/"Sort by Item Group" button (no separate
 * on/off label prefix) via {@link CycleButton.Builder#displayOnlyValue()}.
 */
public class QuickSortOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;

	private final Screen parent;

	public QuickSortOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.quick_sort_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.booleanBuilder(
						Component.translatable("gui.tntsallin1client.quick_sort_options.mode.category"),
						Component.translatable("gui.tntsallin1client.quick_sort_options.mode.name"),
						config.quickSortGroupByCategory)
				.displayOnlyValue()
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.empty(),
						(button, value) -> {
							config.quickSortGroupByCategory = value;
							config.save();
						}));
		y += ROW_HEIGHT + 6;

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
