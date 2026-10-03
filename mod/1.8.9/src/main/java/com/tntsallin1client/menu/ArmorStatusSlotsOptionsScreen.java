package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.ArmorStatusHud;
import com.tntsallin1client.hud.ArmorStatusSlot;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * Shows/hides each Armor & Tool Status slot and sets their order: one on/off row per slot, with an
 * up and a down button that swap it with its neighbor. The rows are in the order the display uses
 * ({@link ArmorStatusHud#orderedSlots}).
 */
public class ArmorStatusSlotsOptionsScreen extends ClientScreen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int ARROW_WIDTH = 20;
	private static final int ARROW_GAP = 2;
	private static final int TOGGLE_WIDTH = ROW_WIDTH - 2 * ARROW_WIDTH - 2 * ARROW_GAP;
	private static final int FIRST_ROW_Y = 40;
	private static final int BACK_BUTTON_ID = 0;
	private static final int HUD_EDITOR_BUTTON_ID = 1;
	/** Each plus the row's index, top to bottom. */
	private static final int FIRST_TOGGLE_ID = 100;
	private static final int FIRST_UP_ID = 200;
	private static final int FIRST_DOWN_ID = 300;

	private final List<ButtonWidget> toggles = new ArrayList<ButtonWidget>();

	public ArmorStatusSlotsOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.armor_status_slots_options.title");
	}

	@Override
	public void init() {
		this.toggles.clear();
		int rows = ArmorStatusSlot.values().length;
		int x = (this.width - ROW_WIDTH) / 2;
		int y = FIRST_ROW_Y;
		for (int row = 0; row < rows; row++) {
			ButtonWidget toggle = new ButtonWidget(FIRST_TOGGLE_ID + row, x, y, TOGGLE_WIDTH, ROW_HEIGHT, "");
			this.toggles.add(toggle);
			this.buttons.add(toggle);

			int upX = x + TOGGLE_WIDTH + ARROW_GAP;
			ButtonWidget up = new ButtonWidget(FIRST_UP_ID + row, upX, y, ARROW_WIDTH, ROW_HEIGHT, "▲");
			up.active = row > 0;
			this.buttons.add(up);
			ButtonWidget down = new ButtonWidget(FIRST_DOWN_ID + row, upX + ARROW_WIDTH + ARROW_GAP, y, ARROW_WIDTH, ROW_HEIGHT, "▼");
			down.active = row < rows - 1;
			this.buttons.add(down);

			y += ROW_SPACING;
		}
		updateLabels();

		// Like on the other options screens of a feature with something on the HUD: "Move / Resize HUD" directly above "Back".
		this.buttons.add(new ButtonWidget(HUD_EDITOR_BUTTON_ID, x, this.height - 52, ROW_WIDTH, ROW_HEIGHT,
				I18n.translate("gui.tntsallin1client.menu.hud_editor_button")));
		this.buttons.add(new ButtonWidget(BACK_BUTTON_ID, x, this.height - 28, ROW_WIDTH, ROW_HEIGHT, I18n.translate("gui.back")));
	}

	/** The rows stay where they are - after a change of order or a switch each just gets the slot now at its place. */
	private void updateLabels() {
		ClientConfig config = ClientConfig.get();
		List<ArmorStatusSlot> slots = ArmorStatusHud.orderedSlots(config);
		for (int row = 0; row < this.toggles.size(); row++) {
			ArmorStatusSlot slot = slots.get(row);
			this.toggles.get(row).message = MenuText.onOff(slot.labelKey(), ArmorStatusHud.isSlotEnabled(config, slot));
		}
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK_BUTTON_ID) {
			back();
			return;
		}
		if (button.id == HUD_EDITOR_BUTTON_ID) {
			this.client.setScreen(new HudEditorScreen(this));
			return;
		}

		ClientConfig config = ClientConfig.get();
		List<ArmorStatusSlot> slots = ArmorStatusHud.orderedSlots(config);
		if (button.id >= FIRST_DOWN_ID) {
			swap(config, slots, button.id - FIRST_DOWN_ID, 1);
		} else if (button.id >= FIRST_UP_ID) {
			swap(config, slots, button.id - FIRST_UP_ID, -1);
		} else {
			ArmorStatusSlot slot = slots.get(button.id - FIRST_TOGGLE_ID);
			config.armorStatusSlotEnabled.put(slot.name(), !ArmorStatusHud.isSlotEnabled(config, slot));
		}
		config.save();
		updateLabels();
	}

	private static void swap(ClientConfig config, List<ArmorStatusSlot> slots, int row, int delta) {
		int target = row + delta;
		if (target < 0 || target >= slots.size()) {
			return;
		}
		Collections.swap(slots, row, target);
		List<String> names = new ArrayList<String>(slots.size());
		for (ArmorStatusSlot slot : slots) {
			names.add(slot.name());
		}
		config.armorStatusSlotOrder = names;
	}
}
