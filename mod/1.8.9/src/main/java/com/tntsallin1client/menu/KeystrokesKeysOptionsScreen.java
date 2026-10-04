package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.KeystrokeKey;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * Shows/hides individual keystrokes boxes, click-to-toggle directly on a copy of the overlay's own
 * row layout ({@link KeystrokeKey#ROWS}) instead of a plain settings list - the toggle UI looks like
 * the thing it's toggling. Switched-off boxes stay visible, just dimmed, so there's always something
 * to click to turn a key back on.
 */
public class KeystrokesKeysOptionsScreen extends ClientScreen {
	private static final int BOX_HEIGHT = 20;
	private static final int BOX_PADDING = 14;
	private static final int GAP = 4;
	private static final int FIRST_ROW_Y = 40;
	private static final int BOX_COLOR_ON = 0x8033CC33;
	private static final int BOX_COLOR_OFF = 0x80333333;
	private static final int TEXT_COLOR_ON = 0xFFFFFF;
	private static final int TEXT_COLOR_OFF = 0x999999;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BACK_BUTTON_ID = 0;
	private static final int HUD_EDITOR_BUTTON_ID = 1;
	private static final int LEFT_MOUSE_BUTTON = 0;

	private final List<KeyBox> boxes = new ArrayList<KeyBox>();

	public KeystrokesKeysOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.keystrokes_keys_options.title");
	}

	@Override
	public void init() {
		this.boxes.clear();
		int rowY = FIRST_ROW_Y;
		for (List<KeystrokeKey> row : KeystrokeKey.ROWS) {
			int x = (this.width - rowWidth(row)) / 2;
			for (KeystrokeKey key : row) {
				int width = boxWidth(key);
				this.boxes.add(new KeyBox(key, x, rowY, width));
				x += width + GAP;
			}
			rowY += BOX_HEIGHT + GAP;
		}

		// Like on the other options screens of a feature with something on the HUD: "Move / Resize HUD"
		// and "Back" follow below the keys, as in the Fabric versions.
		int buttonY = rowY + 6;
		this.buttons.add(new ButtonWidget(HUD_EDITOR_BUTTON_ID, (this.width - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
				I18n.translate("gui.tntsallin1client.menu.hud_editor_button")));
		this.buttons.add(new ButtonWidget(BACK_BUTTON_ID, (this.width - BUTTON_WIDTH) / 2, buttonY + BUTTON_HEIGHT + GAP, BUTTON_WIDTH, BUTTON_HEIGHT,
				I18n.translate("gui.back")));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK_BUTTON_ID) {
			back();
		} else if (button.id == HUD_EDITOR_BUTTON_ID) {
			this.client.setScreen(new HudEditorScreen(this));
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);
		ClientConfig config = ClientConfig.get();
		for (KeyBox box : this.boxes) {
			boolean shown = box.key.isShown(config);
			fill(box.x, box.y, box.x + box.width, box.y + BOX_HEIGHT, shown ? BOX_COLOR_ON : BOX_COLOR_OFF);
			this.drawCenteredString(this.textRenderer, box.key.label, box.x + box.width / 2, box.y + (BOX_HEIGHT - this.textRenderer.fontHeight) / 2,
					shown ? TEXT_COLOR_ON : TEXT_COLOR_OFF);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == LEFT_MOUSE_BUTTON) {
			for (KeyBox box : this.boxes) {
				if (mouseX >= box.x && mouseX < box.x + box.width && mouseY >= box.y && mouseY < box.y + BOX_HEIGHT) {
					ClientConfig config = ClientConfig.get();
					config.keystrokesKeyEnabled.put(box.key.name(), !box.key.isShown(config));
					config.save();
					return;
				}
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	private int rowWidth(List<KeystrokeKey> row) {
		int width = 0;
		for (KeystrokeKey key : row) {
			width += boxWidth(key);
		}
		return width + (row.size() - 1) * GAP;
	}

	private int boxWidth(KeystrokeKey key) {
		return this.textRenderer.getStringWidth(key.label) + BOX_PADDING;
	}

	private static final class KeyBox {
		final KeystrokeKey key;
		final int x;
		final int y;
		final int width;

		KeyBox(KeystrokeKey key, int x, int y, int width) {
			this.key = key;
			this.x = x;
			this.y = y;
			this.width = width;
		}
	}
}
