package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5e, extended Phase 5t, renamed 5ag ("Material Counter" -> "Item Counter"): dedicated
 * options screen for the item counter feature only. Each feature that grows beyond a simple
 * on/off toggle gets its own screen like this one, reached from
 * {@link ClientMenuScreen}, rather than a single shared options screen for
 * every feature - keeps each one focused instead of turning into one long,
 * unrelated settings list. Includes the text color via the shared
 * {@link ColorPickerPanel}.
 */
public class ItemCounterOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private @Nullable ColorPickerPanel colorPicker;

	public ItemCounterOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.item_counter_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		EditBox itemIdBox = new EditBox(this.font, x, y + ROW_SPACING, ROW_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.item_counter_options.item_id"));
		itemIdBox.setMaxLength(64);
		itemIdBox.setValue(config.itemCounterItemId);
		itemIdBox.setEditable(!config.itemCounterUseHeldItem);
		itemIdBox.active = !config.itemCounterUseHeldItem;
		itemIdBox.setResponder(value -> {
			config.itemCounterItemId = value;
			config.save();
		});

		this.addRenderableWidget(CycleButton.booleanBuilder(
						Component.translatable("gui.tntsallin1client.item_counter_options.source.held"),
						Component.translatable("gui.tntsallin1client.item_counter_options.source.fixed"),
						config.itemCounterUseHeldItem)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.item_counter_options.source"),
						(button, value) -> {
							config.itemCounterUseHeldItem = value;
							config.save();
							itemIdBox.setEditable(!value);
							itemIdBox.active = !value;
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(itemIdBox);
		y += ROW_SPACING + 6;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.itemCounterShowItemIcon)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.item_counter_options.show_item_icon"),
						(button, value) -> {
							config.itemCounterShowItemIcon = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, config.itemCounterTextColor,
				this::addRenderableWidget,
				argb -> {
					config.itemCounterTextColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.colorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.colorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.colorPicker.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
