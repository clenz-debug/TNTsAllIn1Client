package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Phase 5e: dedicated options screen for the coordinates HUD - lets each of the
 * three displayed pieces (coordinates, N/S/W/O direction, degree number) be
 * toggled independently, so e.g. "just the coordinates" or "just the compass"
 * both work.
 */
public class CoordinatesHudOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public CoordinatesHudOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.coordinates_hud_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.coordinatesHudShowCoordinates)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.coordinates_hud_options.show_coordinates"),
						(button, value) -> {
							config.coordinatesHudShowCoordinates = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.coordinatesHudShowDirection)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.coordinates_hud_options.show_direction"),
						(button, value) -> {
							config.coordinatesHudShowDirection = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.coordinatesHudShowDegrees)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.coordinates_hud_options.show_degrees"),
						(button, value) -> {
							config.coordinatesHudShowDegrees = value;
							config.save();
						}));
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
