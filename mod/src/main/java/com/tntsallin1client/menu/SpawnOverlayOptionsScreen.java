package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5j redesign: dedicated options screen for the mob-spawn overlay -
 * key rebind (same "click, then press a key; Escape unbinds" pattern as
 * {@link ZoomOptionsScreen}/{@link ShulkerPreviewOptionsScreen}) plus a
 * hold-vs-toggle mode switch, requested alongside the visual redesign itself.
 */
public class SpawnOverlayOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private boolean awaitingKey;

	public SpawnOverlayOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.spawn_overlay_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.rebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKey = true;
					this.updateRebindButtonLabel();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabel();
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.booleanBuilder(
						Component.translatable("gui.tntsallin1client.spawn_overlay_options.mode.hold"),
						Component.translatable("gui.tntsallin1client.spawn_overlay_options.mode.toggle"),
						config.spawnOverlayHoldMode)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.spawn_overlay_options.mode"),
						(button, value) -> {
							config.spawnOverlayHoldMode = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private void updateRebindButtonLabel() {
		Component keyName = ModKeyBindings.SPAWN_OVERLAY.getTranslatedKeyMessage();
		Component label = Component.translatable("gui.tntsallin1client.spawn_overlay_options.key", keyName);
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
			ModKeyBindings.SPAWN_OVERLAY.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKey) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			ModKeyBindings.SPAWN_OVERLAY.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
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
