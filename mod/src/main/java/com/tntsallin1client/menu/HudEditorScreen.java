package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.CoordinatesHud;
import com.tntsallin1client.hud.FpsCounterHud;
import com.tntsallin1client.hud.HudLayout;
import com.tntsallin1client.hud.ItemCounterHud;
import com.tntsallin1client.hud.KeystrokesHud;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lets HUD elements (coordinates HUD, material counter) be dragged to any
 * position and resized by dragging their bottom-right handle, instead of only
 * being toggled on/off. Deliberately not a normal {@link Screen}-with-dark-
 * background: {@link #renderBackground} is a no-op so the real game HUD keeps
 * rendering live underneath our drag handles (Minecraft renders the HUD before
 * the open screen every frame regardless of whether a screen is open, so this
 * works without any extra plumbing).
 */
public class HudEditorScreen extends Screen {
	private static final int HANDLE_SIZE = 6;
	private static final int PADDING = 2;
	private static final float MIN_SCALE = 0.5f;
	private static final float MAX_SCALE = 4.0f;

	private final Screen parent;
	private final List<Entry> entries = new ArrayList<>();
	private Entry dragging;
	private boolean resizing;
	private float dragUnscaledWidth;

	public HudEditorScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.hud_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		entries.clear();
		ClientConfig config = ClientConfig.get();
		entries.add(new Entry(
				Component.translatable("gui.tntsallin1client.menu.coordinates_hud"),
				config.coordinatesHudLayout,
				this::coordinatesBounds));
		entries.add(new Entry(
				Component.translatable("gui.tntsallin1client.menu.item_counter"),
				config.itemCounterHudLayout,
				this::itemCounterBounds));
		entries.add(new Entry(
				Component.translatable("gui.tntsallin1client.menu.fps_counter"),
				config.fpsCounterHudLayout,
				this::fpsCounterBounds));
		entries.add(new Entry(
				Component.translatable("gui.tntsallin1client.menu.keystrokes"),
				config.keystrokesHudLayout,
				this::keystrokesBounds));

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.tntsallin1client.hud_editor.reset_all"),
						button -> resetAll())
				.bounds(this.width / 2 - 154, this.height - 28, 150, 20)
				.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(this.width / 2 + 4, this.height - 28, 150, 20)
				.build());
	}

	@Override
	public void renderBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Intentionally empty: keep the live game HUD visible, unlike a normal darkened screen.
	}

	@Override
	public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		for (Entry entry : entries) {
			Rect bounds = entry.boundsSupplier.get();
			if (bounds == null) {
				continue;
			}

			int fillColor = entry == dragging ? 0x8033CC33 : 0x803388CC;
			guiGraphics.fill(bounds.x - PADDING, bounds.y - PADDING, bounds.x + bounds.width + PADDING, bounds.y + bounds.height + PADDING, fillColor);
			guiGraphics.fill(
					bounds.x + bounds.width - HANDLE_SIZE, bounds.y + bounds.height - HANDLE_SIZE,
					bounds.x + bounds.width + PADDING, bounds.y + bounds.height + PADDING,
					0xFFFFFFFF);
			guiGraphics.drawString(this.font, entry.label, bounds.x, bounds.y - this.font.lineHeight - 2, 0xFFFFFF55);
		}

		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFFFF);
		guiGraphics.drawCenteredString(this.font, Component.translatable("gui.tntsallin1client.hud_editor.hint"), this.width / 2, 6 + this.font.lineHeight + 2, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Entry entry : entries) {
				Rect bounds = entry.boundsSupplier.get();
				if (bounds == null) {
					continue;
				}

				boolean onHandle = inRect(event.x(), event.y(),
						bounds.x + bounds.width - HANDLE_SIZE, bounds.y + bounds.height - HANDLE_SIZE,
						bounds.x + bounds.width + PADDING, bounds.y + bounds.height + PADDING);
				boolean onBody = inRect(event.x(), event.y(), bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
				if (!onHandle && !onBody) {
					continue;
				}

				entry.layout.customPosition = true;
				entry.layout.x = bounds.x;
				entry.layout.y = bounds.y;
				dragging = entry;
				resizing = onHandle;
				dragUnscaledWidth = bounds.width / entry.layout.scale;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging != null) {
			if (resizing) {
				float newScale = (float) (event.x() - dragging.layout.x) / dragUnscaledWidth;
				dragging.layout.scale = Mth.clamp(newScale, MIN_SCALE, MAX_SCALE);
			} else {
				dragging.layout.x += (float) dragX;
				dragging.layout.y += (float) dragY;
			}
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			ClientConfig.get().save();
			dragging = null;
			resizing = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private void resetAll() {
		ClientConfig config = ClientConfig.get();
		config.coordinatesHudLayout = new HudLayout();
		config.itemCounterHudLayout = new HudLayout();
		config.fpsCounterHudLayout = new HudLayout();
		config.keystrokesHudLayout = new HudLayout();
		config.save();
	}

	private Rect coordinatesBounds() {
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return null;
		}

		ClientConfig config = ClientConfig.get();
		List<String> lines = CoordinatesHud.buildLines(config, player);
		if (lines.isEmpty()) {
			return null;
		}

		HudLayout layout = config.coordinatesHudLayout;
		float x = layout.customPosition ? layout.x : CoordinatesHud.DEFAULT_LEFT;
		float y = layout.customPosition ? layout.y : CoordinatesHud.DEFAULT_TOP;

		int unscaledWidth = 0;
		for (String line : lines) {
			unscaledWidth = Math.max(unscaledWidth, this.font.width(line));
		}
		int unscaledHeight = lines.size() * this.font.lineHeight;

		return new Rect(Math.round(x), Math.round(y), Math.round(unscaledWidth * layout.scale), Math.round(unscaledHeight * layout.scale));
	}

	private Rect itemCounterBounds() {
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return null;
		}

		ClientConfig config = ClientConfig.get();
		String label = ItemCounterHud.buildLabel(config, player);
		if (label == null) {
			// Nothing to actually count right now (e.g. "track held item" with an empty hand) - the real
			// HUD correctly shows nothing then, but the editor still needs a box to drag/resize, otherwise
			// positioning this element is only possible while holding a trackable item.
			label = Component.translatable("gui.tntsallin1client.menu.item_counter").getString();
		}

		boolean showIcon = config.itemCounterShowItemIcon;
		HudLayout layout = config.itemCounterHudLayout;
		float x = layout.customPosition ? layout.x : ItemCounterHud.defaultX(this.width, this.font, label, showIcon);
		float y = layout.customPosition ? layout.y : ItemCounterHud.defaultY();

		int unscaledWidth = ItemCounterHud.contentWidth(this.font, label, showIcon);
		int unscaledHeight = ItemCounterHud.contentHeight(this.font, showIcon);

		return new Rect(Math.round(x), Math.round(y), Math.round(unscaledWidth * layout.scale), Math.round(unscaledHeight * layout.scale));
	}

	private Rect fpsCounterBounds() {
		ClientConfig config = ClientConfig.get();
		String label = FpsCounterHud.buildLabel(this.minecraft);

		HudLayout layout = config.fpsCounterHudLayout;
		float x = layout.customPosition ? layout.x : FpsCounterHud.defaultX(this.width, this.font, label);
		float y = layout.customPosition ? layout.y : FpsCounterHud.defaultY();

		int unscaledWidth = this.font.width(label);
		int unscaledHeight = this.font.lineHeight;

		return new Rect(Math.round(x), Math.round(y), Math.round(unscaledWidth * layout.scale), Math.round(unscaledHeight * layout.scale));
	}

	private Rect keystrokesBounds() {
		if (this.minecraft.player == null) {
			return null;
		}

		ClientConfig config = ClientConfig.get();
		HudLayout layout = config.keystrokesHudLayout;
		int unscaledWidth = KeystrokesHud.computeTotalWidth(this.minecraft);
		int unscaledHeight = KeystrokesHud.computeTotalHeight(this.minecraft);
		float x = layout.customPosition ? layout.x : this.width - 4 - unscaledWidth;
		float y = layout.customPosition ? layout.y : this.height - 4 - unscaledHeight;

		return new Rect(Math.round(x), Math.round(y), Math.round(unscaledWidth * layout.scale), Math.round(unscaledHeight * layout.scale));
	}

	private static boolean inRect(double x, double y, int x1, int y1, int x2, int y2) {
		return x >= x1 && x < x2 && y >= y1 && y < y2;
	}

	private record Rect(int x, int y, int width, int height) {
	}

	private record Entry(Component label, HudLayout layout, Supplier<Rect> boundsSupplier) {
	}
}
