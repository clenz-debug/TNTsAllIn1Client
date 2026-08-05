package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5ah: dedicated options screen for the pinned-recipe feature. Rebind capture logic is
 * identical to {@link ZoomOptionsScreen}'s (itself mirroring vanilla's own Controls screen
 * convention) - both edit the same {@link ModKeyBindings#PIN_RECIPE} KeyMapping.
 */
public class PinnedRecipeOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private @Nullable Button unpinButton;
	private boolean awaitingKey;

	public PinnedRecipeOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.pinned_recipe_options.title"));
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
		y += ROW_SPACING + 16;

		this.unpinButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.pinned_recipe_options.unpin_button"),
						button -> {
							ClientConfig config = ClientConfig.get();
							config.pinnedRecipe = null;
							config.save();
							this.updateUnpinButtonState();
						})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateUnpinButtonState();
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		guiGraphics.drawCenteredString(this.font, Component.translatable("gui.tntsallin1client.pinned_recipe_options.hint"),
				this.width / 2, 40 + ROW_SPACING, 0xFFAAAAAA);
	}

	private void updateRebindButtonLabel() {
		Component keyName = ModKeyBindings.PIN_RECIPE.getTranslatedKeyMessage();
		Component label = Component.translatable("gui.tntsallin1client.pinned_recipe_options.key", keyName);
		if (this.awaitingKey) {
			label = Component.literal("> ")
					.append(label.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
		}
		this.rebindButton.setMessage(label);
	}

	private void updateUnpinButtonState() {
		this.unpinButton.active = ClientConfig.get().pinnedRecipe != null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.awaitingKey) {
			ModKeyBindings.PIN_RECIPE.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKey) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			ModKeyBindings.PIN_RECIPE.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
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
