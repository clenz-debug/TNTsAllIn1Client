package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.ArmorStatusHud;
import com.tntsallin1client.hud.ArmorStatusSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Shows/hides each of the six Armor & Tool Status slots independently - one
 * on/off row per {@link ArmorStatusSlot}, same "absent from the map = enabled"
 * convention as {@link com.tntsallin1client.hud.KeystrokeKey}/
 * {@link KeystrokesKeysOptionsScreen}. Six plain toggle rows fit on screen at
 * every GUI scale without needing to scroll, unlike {@link ArmorStatusOptionsScreen}.
 */
public class ArmorStatusSlotsOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public ArmorStatusSlotsOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.armor_status_slots_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
			this.addRenderableWidget(CycleButton.onOffBuilder(ArmorStatusHud.isSlotEnabled(config, slot))
					.create(x, y, ROW_WIDTH, ROW_HEIGHT,
							Component.translatable("gui.tntsallin1client.armor_status_slot." + slot.name().toLowerCase(Locale.ROOT)),
							(button, value) -> {
								config.armorStatusSlotEnabled.put(slot.name(), value);
								config.save();
							}));
			y += ROW_SPACING;
		}
		y += 4;

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
