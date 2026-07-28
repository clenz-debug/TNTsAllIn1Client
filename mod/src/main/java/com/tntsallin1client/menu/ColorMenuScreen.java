package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Phase 5t: central color settings screen, reached via its own button from
 * {@link ClientMenuScreen} rather than a per-feature toggle+options row.
 * Every color setting in the mod lives here now - crosshair/target/hitbox/
 * block-outline/keystrokes colors used to each live in their own feature's
 * options screen (one {@link ColorPickerPanel} apiece), and the plain-text
 * HUD/overlay elements had no configurable color at all. Moved out on user
 * request once several more text colors were about to be added, each of
 * which would otherwise have meant one more color picker scattered across
 * yet another feature's own screen - colors are a cross-cutting concern, not
 * a per-feature setting, so they get one shared screen instead. Every row
 * opens the same generic {@link SingleColorOptionsScreen}; the small square
 * next to each label is a live preview of that color.
 */
public class ColorMenuScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 24;
	private static final int SWATCH_SIZE = 14;
	private static final int SWATCH_GAP = 6;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 30;

	private final Screen parent;

	public ColorMenuScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.color_menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		ColorList list = new ColorList(this.minecraft, this.width, listHeight, LIST_TOP);

		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.coordinates_hud"),
				config.coordinatesHudTextColor, argb -> {
					config.coordinatesHudTextColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.material_counter"),
				config.materialCounterTextColor, argb -> {
					config.materialCounterTextColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.fps_counter"),
				config.fpsCounterTextColor, argb -> {
					config.fpsCounterTextColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.client_name_label"),
				config.clientNameLabelColor, argb -> {
					config.clientNameLabelColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.f3_system_info"),
				config.systemInfoTextColor, argb -> {
					config.systemInfoTextColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.crosshair"),
				config.customCrosshairColor, argb -> {
					config.customCrosshairColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.crosshair_target"),
				config.crosshairTargetColor, argb -> {
					config.crosshairTargetColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.hitbox"),
				config.customHitboxColor, argb -> {
					config.customHitboxColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.block_outline"),
				config.customBlockOutlineColor, argb -> {
					config.customBlockOutlineColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.keystrokes_active"),
				config.keystrokesActiveColor, argb -> {
					config.keystrokesActiveColor = argb;
					config.save();
				});
		addColor(list, Component.translatable("gui.tntsallin1client.color_menu.keystrokes_text"),
				config.keystrokesTextColor, argb -> {
					config.keystrokesTextColor = argb;
					config.save();
				});

		this.addRenderableWidget(list);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds((this.width - ROW_WIDTH) / 2, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private void addColor(ColorList list, Component label, int initialColor, IntConsumer onChange) {
		list.addColorRow(label, initialColor, () -> new SingleColorOptionsScreen(this, label, initialColor, onChange));
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

	/** Scrollable row list - same {@link ContainerObjectSelectionList} shape as {@link ClientMenuScreen}'s own feature list. */
	private static class ColorList extends ContainerObjectSelectionList<ColorList.Row> {
		ColorList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		void addColorRow(Component label, int currentColor, Supplier<Screen> screenFactory) {
			Button button = Button.builder(label, b -> Minecraft.getInstance().setScreen(screenFactory.get()))
					.bounds(0, 0, ROW_WIDTH, ROW_HEIGHT)
					.build();
			this.addEntry(new Row(button, currentColor));
		}

		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final Button button;
			private final int color;

			Row(Button button, int color) {
				this.button = button;
				this.color = color;
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				this.button.setPosition(this.getContentX(), this.getContentY());
				this.button.render(guiGraphics, mouseX, mouseY, partialTick);
				int swatchX = this.getContentX() + ROW_WIDTH - SWATCH_SIZE - SWATCH_GAP;
				int swatchY = this.getContentY() + (ROW_HEIGHT - SWATCH_SIZE) / 2;
				ColorPickerHelper.drawSwatch(guiGraphics, swatchX, swatchY, SWATCH_SIZE, this.color);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.button);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.button);
			}
		}
	}
}
