package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Phase 5d, extended Phase 5t: dedicated options screen for the F3 features -
 * the F3+&lt;key&gt; binding for the system info page ({@link SystemInfoOverlay}),
 * plus that page's text color via the shared {@link ColorPickerPanel}. The
 * key can be changed from here directly instead of only via vanilla's
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
	private @Nullable ColorPickerPanel colorPicker;
	private boolean awaitingKey;

	public F3OptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.f3_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.f3QuickInfoEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.f3_options.enabled"),
						(button, value) -> {
							config.f3QuickInfoEnabled = value;
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
		y += ROW_SPACING + 6;

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.systemInfoTextColor,
				this::addRenderableWidget,
				argb -> {
					config.systemInfoTextColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 6;

		OptionsChrome.add(this, x, y, ROW_WIDTH, this::onClose, this::addRenderableWidget, this::addWidget);
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
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.awaitingKey) {
			ModKeyBindings.SYSTEM_INFO.setKey(InputConstants.Type.MOUSE.getOrCreate(button));
			finishRebind();
			return true;
		}
		if (this.colorPicker.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.colorPicker.mouseDragged(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.colorPicker.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.awaitingKey) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			ModKeyBindings.SYSTEM_INFO.setKey(keyCode == GLFW.GLFW_KEY_ESCAPE ? InputConstants.UNKNOWN : InputConstants.getKey(keyCode, scanCode));
			finishRebind();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void finishRebind() {
		this.awaitingKey = false;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.updateRebindButtonLabel();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
