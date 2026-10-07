package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.ArmorStatusHud;
import com.tntsallin1client.hud.ArmorStatusSlot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

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

	private final List<Button> toggles = new ArrayList<Button>();

	public ArmorStatusSlotsOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.armor_status_slots_options.title");
	}

	@Override
	protected void init() {
		this.toggles.clear();
		int rows = ArmorStatusSlot.values().length;
		int x = (this.width - ROW_WIDTH) / 2;
		int y = FIRST_ROW_Y;
		for (int index = 0; index < rows; index++) {
			final int row = index;
			this.toggles.add(this.addButton(new Button(x, y, TOGGLE_WIDTH, ROW_HEIGHT, "", pressed -> toggle(row))));
			int upX = x + TOGGLE_WIDTH + ARROW_GAP;
			Button up = this.addButton(new Button(upX, y, ARROW_WIDTH, ROW_HEIGHT, "▲", pressed -> move(row, -1)));
			up.active = row > 0;
			Button down = this.addButton(new Button(upX + ARROW_WIDTH + ARROW_GAP, y, ARROW_WIDTH, ROW_HEIGHT, "▼", pressed -> move(row, 1)));
			down.active = row < rows - 1;
			y += ROW_SPACING;
		}
		updateLabels();

		// Like on the other options screens of a feature with something on the HUD: "Move / Resize HUD"
		// and "Back" continue the column - in the client design they are the top bar.
		if (TopBar.inUse()) {
			this.addButton(TopBar.back(I18n.get("gui.back"), this::back));
			this.addButton(TopBar.hudEditor(this.width, this::openHudEditor));
			return;
		}
		y += 4;
		this.addButton(new Button(x, y, ROW_WIDTH, ROW_HEIGHT, I18n.get("gui.tntsallin1client.menu.hud_editor_button"), pressed -> openHudEditor()));
		this.addButton(new Button(x, y + ROW_SPACING, ROW_WIDTH, ROW_HEIGHT, I18n.get("gui.back"), pressed -> back()));
	}

	private void openHudEditor() {
		this.minecraft.setScreen(new HudEditorScreen(this));
	}

	/** The rows stay where they are - after a change of order or a switch each just gets the slot now at its place. */
	private void updateLabels() {
		ClientConfig config = ClientConfig.get();
		List<ArmorStatusSlot> slots = ArmorStatusHud.orderedSlots(config);
		for (int row = 0; row < this.toggles.size(); row++) {
			ArmorStatusSlot slot = slots.get(row);
			this.toggles.get(row).setMessage(MenuText.onOff(slot.labelKey(), ArmorStatusHud.isSlotEnabled(config, slot)));
		}
	}

	private void toggle(int row) {
		ClientConfig config = ClientConfig.get();
		ArmorStatusSlot slot = ArmorStatusHud.orderedSlots(config).get(row);
		config.armorStatusSlotEnabled.put(slot.name(), !ArmorStatusHud.isSlotEnabled(config, slot));
		config.save();
		updateLabels();
	}

	/** Swaps the slot in `row` with its neighbor above (`delta` -1) or below (1). */
	private void move(int row, int delta) {
		ClientConfig config = ClientConfig.get();
		List<ArmorStatusSlot> slots = ArmorStatusHud.orderedSlots(config);
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
		config.save();
		updateLabels();
	}
}
