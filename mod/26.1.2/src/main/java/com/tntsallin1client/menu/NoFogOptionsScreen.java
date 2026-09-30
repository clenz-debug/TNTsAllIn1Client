package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Options for the "No fog" row: which fog types get removed. Blindness and darkness fog aren't
 * listed on purpose - they're status effects, not scenery.
 */
public class NoFogOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public NoFogOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.no_fog_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		y = addRow(x, y, "gui.tntsallin1client.no_fog_options.enabled", config.noFogEnabled, value -> config.noFogEnabled = value);
		y = addRow(x, y, "gui.tntsallin1client.no_fog_options.distance", config.noFogDistance, value -> config.noFogDistance = value);
		y = addRow(x, y, "gui.tntsallin1client.no_fog_options.water", config.noFogWater, value -> config.noFogWater = value);
		y = addRow(x, y, "gui.tntsallin1client.no_fog_options.lava", config.noFogLava, value -> config.noFogLava = value);
		y = addRow(x, y, "gui.tntsallin1client.no_fog_options.powder_snow", config.noFogPowderSnow, value -> config.noFogPowderSnow = value);
		y += 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private int addRow(int x, int y, String labelKey, boolean value, Consumer<Boolean> setter) {
		this.addRenderableWidget(CycleButton.onOffBuilder(value)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable(labelKey),
						(button, newValue) -> {
							setter.accept(newValue);
							ClientConfig.get().save();
						}));
		return y + ROW_SPACING;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
