package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5l: dedicated options screen for the F3+B hitbox color - same
 * {@link ColorPickerPanel} as {@link CrosshairOptionsScreen}.
 */
public class HitboxColorOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;

	private final Screen parent;
	private @Nullable ColorPickerPanel colorPicker;

	public HitboxColorOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.hitbox_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.customHitboxColor,
				this::addRenderableWidget,
				argb -> {
					config.customHitboxColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
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
