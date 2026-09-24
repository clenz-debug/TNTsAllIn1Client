package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.ArmorStatusHud;
import com.tntsallin1client.hud.ArmorStatusSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Shows/hides each of the six Armor & Tool Status slots independently - one
 * on/off row per {@link ArmorStatusSlot}, same "absent from the map = enabled"
 * convention as {@link com.tntsallin1client.hud.KeystrokeKey}/
 * {@link KeystrokesKeysOptionsScreen}. Six plain toggle rows fit on screen at
 * every GUI scale without needing to scroll, unlike {@link ArmorStatusOptionsScreen}.
 *
 * <p>Rows follow {@link ArmorStatusHud#orderedSlots} - the {@code ▲}/{@code ▼}
 * buttons on each row swap it with its neighbor in {@link ClientConfig#armorStatusSlotOrder}
 * and rebuild, so this screen doubles as the editor for the order the bundled
 * layout (and this list itself) uses, per user request: previously a slot like
 * main-hand always rendered wherever {@link ArmorStatusSlot}'s fixed declaration
 * order put it (e.g. always last/bottom), with no way to move it.</p>
 */
public class ArmorStatusSlotsOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int ARROW_WIDTH = 20;
	private static final int ARROW_GAP = 2;
	private static final int TOGGLE_WIDTH = ROW_WIDTH - 2 * ARROW_WIDTH - 2 * ARROW_GAP;

	private final Screen parent;

	public ArmorStatusSlotsOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.armor_status_slots_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		List<ArmorStatusSlot> slots = ArmorStatusHud.orderedSlots(config);
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		for (int i = 0; i < slots.size(); i++) {
			ArmorStatusSlot slot = slots.get(i);

			this.addRenderableWidget(CycleButton.onOffBuilder(ArmorStatusHud.isSlotEnabled(config, slot))
					.create(x, y, TOGGLE_WIDTH, ROW_HEIGHT,
							Component.translatable("gui.tntsallin1client.armor_status_slot." + slot.name().toLowerCase(Locale.ROOT)),
							(button, value) -> {
								config.armorStatusSlotEnabled.put(slot.name(), value);
								config.save();
							}));

			int upX = x + TOGGLE_WIDTH + ARROW_GAP;
			this.addRenderableWidget(Button.builder(Component.literal("▲"), button -> this.moveSlot(config, slot, -1))
					.bounds(upX, y, ARROW_WIDTH, ROW_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.armor_status_slots_options.move_up")))
					.build())
					.active = i > 0;

			int downX = upX + ARROW_WIDTH + ARROW_GAP;
			this.addRenderableWidget(Button.builder(Component.literal("▼"), button -> this.moveSlot(config, slot, 1))
					.bounds(downX, y, ARROW_WIDTH, ROW_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.armor_status_slots_options.move_down")))
					.build())
					.active = i < slots.size() - 1;

			y += ROW_SPACING;
		}
		y += 4;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private void moveSlot(ClientConfig config, ArmorStatusSlot slot, int delta) {
		List<ArmorStatusSlot> order = ArmorStatusHud.orderedSlots(config);
		int index = order.indexOf(slot);
		int target = index + delta;
		if (target < 0 || target >= order.size()) {
			return;
		}
		Collections.swap(order, index, target);

		List<String> names = new ArrayList<>(order.size());
		for (ArmorStatusSlot ordered : order) {
			names.add(ordered.name());
		}
		config.armorStatusSlotOrder = names;
		config.save();
		this.rebuild();
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
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
