package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Options for the "3D Skin Layers" row: which parts of the skin's second layer are drawn in 3D, how
 * far they stand off the body, and up to which distance other players get them at all.
 */
public class SkinLayers3dOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public SkinLayers3dOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.skin_layers_3d_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 32;

		y = addRow(x, y, "gui.tntsallin1client.skin_layers_3d_options.enabled", config.skinLayers3dEnabled, value -> config.skinLayers3dEnabled = value);
		y = addRow(x, y, "gui.tntsallin1client.skin_layers_3d_options.head", config.skinLayers3dHead, value -> config.skinLayers3dHead = value);
		y = addRow(x, y, "gui.tntsallin1client.skin_layers_3d_options.jacket", config.skinLayers3dJacket, value -> config.skinLayers3dJacket = value);
		y = addRow(x, y, "gui.tntsallin1client.skin_layers_3d_options.sleeves", config.skinLayers3dSleeves, value -> config.skinLayers3dSleeves = value);
		y = addRow(x, y, "gui.tntsallin1client.skin_layers_3d_options.pants", config.skinLayers3dPants, value -> config.skinLayers3dPants = value);

		this.addRenderableWidget(new IntSliderButton(x, y, ROW_WIDTH, ROW_HEIGHT,
				SkinLayers3d.MIN_DEPTH_PERCENT, SkinLayers3d.MAX_DEPTH_PERCENT, config.skinLayers3dDepthPercent,
				percent -> Component.translatable("gui.tntsallin1client.skin_layers_3d_options.depth", percent),
				percent -> {
					config.skinLayers3dDepthPercent = percent;
					config.save();
				}));
		y += ROW_SPACING;

		this.addRenderableWidget(new IntSliderButton(x, y, ROW_WIDTH, ROW_HEIGHT,
				SkinLayers3d.MIN_DISTANCE, SkinLayers3d.MAX_DISTANCE, config.skinLayers3dDistance,
				blocks -> Component.translatable("gui.tntsallin1client.skin_layers_3d_options.distance", blocks),
				blocks -> {
					config.skinLayers3dDistance = blocks;
					config.save();
				}));
		y += ROW_SPACING + 6;

		OptionsChrome.add(this, x, y, ROW_WIDTH, this::onClose, this::addRenderableWidget, this::addWidget);
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
