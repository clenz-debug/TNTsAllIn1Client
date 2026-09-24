package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Dedicated options screen for freecam - on/off, rebind key, speed and a sensitivity override
 * separate from the player's normal mouse-look sensitivity. Rebind capture logic is identical to
 * {@link ZoomOptionsScreen}'s (itself mirroring vanilla's own {@code KeyBindsScreen}/{@code
 * KeyBindsList} "click, then press a key; Escape unbinds" convention) - both edit the same
 * {@link ModKeyBindings#TOGGLE_FREECAM} KeyMapping, so this and vanilla's Controls screen can
 * never fall out of sync with each other.
 */
public class FreecamOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int MIN_SPEED = 1;
	private static final int MAX_SPEED = 50;
	private static final int MIN_SENSITIVITY_PERCENT = 10;
	private static final int MAX_SENSITIVITY_PERCENT = 300;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private boolean awaitingKey;

	public FreecamOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.freecam_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.freecamEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.freecam_options.enabled"),
						(button, value) -> {
							config.freecamEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.rebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKey = true;
					this.updateRebindButtonLabel();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabel();
		y += ROW_SPACING;

		this.addRenderableWidget(new IntSliderButton(x, y, ROW_WIDTH, ROW_HEIGHT,
				MIN_SPEED, MAX_SPEED, config.freecamSpeed,
				speed -> Component.translatable("gui.tntsallin1client.freecam_options.speed", speed),
				speed -> {
					config.freecamSpeed = speed;
					config.save();
				}));
		y += ROW_SPACING;

		this.addRenderableWidget(new IntSliderButton(x, y, ROW_WIDTH, ROW_HEIGHT,
				MIN_SENSITIVITY_PERCENT, MAX_SENSITIVITY_PERCENT, config.freecamSensitivityPercent,
				percent -> Component.translatable("gui.tntsallin1client.freecam_options.sensitivity", percent),
				percent -> {
					config.freecamSensitivityPercent = percent;
					config.save();
				}));
		y += ROW_SPACING + 4;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	private void updateRebindButtonLabel() {
		Component keyName = ModKeyBindings.TOGGLE_FREECAM.getTranslatedKeyMessage();
		Component label = Component.translatable("gui.tntsallin1client.freecam_options.key", keyName);
		if (this.awaitingKey) {
			label = Component.literal("> ")
					.append(label.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
		}
		this.rebindButton.setMessage(label);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.awaitingKey) {
			ModKeyBindings.TOGGLE_FREECAM.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKey) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			ModKeyBindings.TOGGLE_FREECAM.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
			finishRebind();
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	private void finishRebind() {
		this.awaitingKey = false;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.updateRebindButtonLabel();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
