package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.KeystrokeKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5m follow-up: shows/hides individual keystrokes boxes, click-to-toggle
 * directly on a copy of the overlay's own row layout ({@link KeystrokeKey#ROWS})
 * instead of a plain settings list - the toggle UI looks like the thing it's
 * toggling. Disabled boxes stay visible, just dimmed, so there's always
 * something to click to turn a key back on (removing them entirely would make
 * a fully-disabled row impossible to re-enable).
 */
public class KeystrokesKeysOptionsScreen extends Screen {
	private static final int BOX_HEIGHT = 20;
	private static final int BOX_PADDING = 14;
	private static final int GAP = 4;
	private static final int ROW_GAP = 4;
	private static final int BOX_COLOR_ON = 0x8033CC33;
	private static final int BOX_COLOR_OFF = 0x80333333;
	private static final int TEXT_COLOR_ON = 0xFFFFFFFF;
	private static final int TEXT_COLOR_OFF = 0xFF999999;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;

	private record KeyBox(KeystrokeKey key, int x, int y, int width) {
	}

	private final Screen parent;
	private List<KeyBox> boxes = List.of();

	public KeystrokesKeysOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.keystrokes_keys_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Font font = this.font;
		List<KeyBox> layout = new ArrayList<>();
		int rowY = 40;
		for (List<KeystrokeKey> row : KeystrokeKey.ROWS) {
			int rowWidth = rowWidth(row, font);
			int x = (this.width - rowWidth) / 2;
			for (KeystrokeKey key : row) {
				int width = boxWidth(key.label, font);
				layout.add(new KeyBox(key, x, rowY, width));
				x += width + GAP;
			}
			rowY += BOX_HEIGHT + ROW_GAP;
		}
		this.boxes = layout;

		int buttonX = (this.width - BUTTON_WIDTH) / 2;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(buttonX, rowY + 6, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

		ClientConfig config = ClientConfig.get();
		for (KeyBox box : this.boxes) {
			boolean enabled = config.keystrokesKeyEnabled.getOrDefault(box.key().name(), true);
			guiGraphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + BOX_HEIGHT, enabled ? BOX_COLOR_ON : BOX_COLOR_OFF);
			guiGraphics.drawCenteredString(this.font, box.key().label, box.x() + box.width() / 2,
					box.y() + (BOX_HEIGHT - this.font.lineHeight) / 2, enabled ? TEXT_COLOR_ON : TEXT_COLOR_OFF);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (KeyBox box : this.boxes) {
			if (event.x() >= box.x() && event.x() < box.x() + box.width() && event.y() >= box.y() && event.y() < box.y() + BOX_HEIGHT) {
				ClientConfig config = ClientConfig.get();
				boolean enabled = config.keystrokesKeyEnabled.getOrDefault(box.key().name(), true);
				config.keystrokesKeyEnabled.put(box.key().name(), !enabled);
				config.save();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static int rowWidth(List<KeystrokeKey> row, Font font) {
		int width = 0;
		for (KeystrokeKey key : row) {
			width += boxWidth(key.label, font);
		}
		return width + (row.size() - 1) * GAP;
	}

	private static int boxWidth(String label, Font font) {
		return font.width(label) + BOX_PADDING;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
