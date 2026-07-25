package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5d: dedicated options screen for the F3 features, currently just the
 * F3+&lt;key&gt; binding for the system info page ({@link SystemInfoOverlay}).
 * Lets that key be changed from here directly instead of only via vanilla's
 * Controls screen - both edit the same {@link ModKeyBindings#SYSTEM_INFO}
 * KeyMapping, so they can never fall out of sync. Rebind capture logic mirrors
 * vanilla's own {@code KeyBindsScreen}/{@code KeyBindsList} (same "click, then
 * press a key; Escape unbinds" convention players already know).
 */
public class F3OptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private boolean awaitingKey;

	public F3OptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.f3_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.rebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKey = true;
					this.updateRebindButtonLabel();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabel();
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private void updateRebindButtonLabel() {
		Component keyName = ModKeyBindings.SYSTEM_INFO.getTranslatedKeyMessage();
		Component label = Component.translatable("gui.tntsallin1client.f3_options.system_info_key", keyName);
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
			ModKeyBindings.SYSTEM_INFO.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKey) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			ModKeyBindings.SYSTEM_INFO.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
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
