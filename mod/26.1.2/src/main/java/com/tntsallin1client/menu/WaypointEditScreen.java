package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Waypoint idea: edit a single waypoint (name, position, color, visibility, beam/marker/distance/fade)
 * or delete it.
 * {@link #waypoint} is the live object inside {@link ClientConfig#waypoints}, not a copy - every
 * field responder writes straight back into it and saves immediately, same pattern as every other
 * settings screen in this mod. Dimension is shown but not editable (always set from where the
 * waypoint was created) - keeps this screen from needing a whole dimension picker for what would
 * be a rare edit; recreating the waypoint while standing in the target dimension covers that case.
 *
 * <p>Taller than a small window at a high GUI scale (the color picker alone is ~200px), so it scrolls
 * the same way {@link CrosshairOptionsScreen} does: {@link #init} lays everything out shifted by
 * {@link #scrollOffset}, scrolling re-runs it ({@link #rebuild}), rendering clips to the viewport
 * below the title and {@link ScrollBarHelper} draws the bar. Unlike there, the mouse wheel always scrolls
 * the page while there is something to scroll - most of the lower half is on/off {@code CycleButton}s,
 * which cycle on the wheel, so scrolling down would otherwise silently flip this waypoint's settings.
 */
public class WaypointEditScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int FIELD_GAP = 6;
	private static final int TOP_MARGIN = 36;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private final Waypoint waypoint;
	private @Nullable ColorPickerPanel colorPicker;
	private @Nullable ScrollBarHelper scrollBar;
	private int dimensionLabelY;
	private int scrollOffset;
	private int maxScroll;

	public WaypointEditScreen(Screen parent, Waypoint waypoint) {
		super(Component.translatable("gui.tntsallin1client.waypoint_edit.title"));
		this.parent = parent;
		this.waypoint = waypoint;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		this.maxScroll = Math.max(0, computeContentHeight(this.font.lineHeight) - viewportHeight);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

		int x = (this.width - ROW_WIDTH) / 2;
		int y = TOP_MARGIN - this.scrollOffset;

		if (this.scrollBar == null) {
			this.scrollBar = new ScrollBarHelper(x + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight,
					() -> this.scrollOffset, () -> this.maxScroll,
					newOffset -> {
						this.scrollOffset = newOffset;
						this.rebuild();
					});
		} else {
			this.scrollBar.reposition(x + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight);
		}

		EditBox nameField = new EditBox(this.font, x, y, ROW_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.waypoint_edit.name"));
		nameField.setMaxLength(24);
		nameField.setValue(this.waypoint.name);
		nameField.setResponder(value -> {
			this.waypoint.name = value;
			config.save();
		});
		this.addRenderableWidget(nameField);
		y += ROW_HEIGHT + FIELD_GAP;

		int coordWidth = (ROW_WIDTH - 2 * FIELD_GAP) / 3;
		this.addRenderableWidget(coordField(x, y, coordWidth,
				Component.translatable("gui.tntsallin1client.waypoint_edit.x"), this.waypoint.x,
				value -> this.waypoint.x = value));
		this.addRenderableWidget(coordField(x + coordWidth + FIELD_GAP, y, coordWidth,
				Component.translatable("gui.tntsallin1client.waypoint_edit.y"), this.waypoint.y,
				value -> this.waypoint.y = value));
		this.addRenderableWidget(coordField(x + 2 * (coordWidth + FIELD_GAP), y, coordWidth,
				Component.translatable("gui.tntsallin1client.waypoint_edit.z"), this.waypoint.z,
				value -> this.waypoint.z = value));
		y += ROW_HEIGHT + FIELD_GAP + 4;

		this.dimensionLabelY = y;
		y += this.font.lineHeight + FIELD_GAP + 4;

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, this.waypoint.color,
				this::addRenderableWidget,
				argb -> {
					this.waypoint.color = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + FIELD_GAP;

		this.addRenderableWidget(CycleButton.onOffBuilder(this.waypoint.visible)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_edit.visible"),
						(button, value) -> {
							this.waypoint.visible = value;
							config.save();
						}));
		y += ROW_HEIGHT + FIELD_GAP;

		// Per-waypoint display settings, two per row so the screen doesn't grow by four full rows.
		int halfWidth = (ROW_WIDTH - FIELD_GAP) / 2;
		int rightX = x + ROW_WIDTH - halfWidth;
		this.addRenderableWidget(displayToggle(x, y, halfWidth, "show_beam", this.waypoint.showBeam,
				value -> this.waypoint.showBeam = value));
		this.addRenderableWidget(displayToggle(rightX, y, halfWidth, "show_marker", this.waypoint.showMarker,
				value -> this.waypoint.showMarker = value));
		y += ROW_HEIGHT + FIELD_GAP;
		this.addRenderableWidget(displayToggle(x, y, halfWidth, "show_distance", this.waypoint.showDistance,
				value -> this.waypoint.showDistance = value));
		this.addRenderableWidget(displayToggle(rightX, y, halfWidth, "fade_nearby", this.waypoint.fadeNearby,
				value -> this.waypoint.fadeNearby = value));
		y += ROW_HEIGHT + FIELD_GAP;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.waypoint_edit.delete_button"),
						button -> {
							if (config.waypointConfirmDelete) {
								this.confirmDelete();
							} else {
								this.deleteWaypoint();
							}
						})
				.bounds(x, y, halfWidth, ROW_HEIGHT)
				.build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(rightX, y, halfWidth, ROW_HEIGHT)
				.build());
	}

	/**
	 * Mirrors the y-cursor arithmetic in {@link #init} to get the unscrolled content height (from
	 * {@link #TOP_MARGIN}) without creating widgets - needed to clamp {@link #scrollOffset} before laying
	 * anything out. Keep in sync with {@link #init} if that layout ever changes.
	 */
	private static int computeContentHeight(int fontLineHeight) {
		int height = ROW_HEIGHT + FIELD_GAP;
		height += ROW_HEIGHT + FIELD_GAP + 4;
		height += fontLineHeight + FIELD_GAP + 4;
		height += ColorPickerPanel.totalHeight() + FIELD_GAP;
		height += 3 * (ROW_HEIGHT + FIELD_GAP);
		height += ROW_HEIGHT;
		return height;
	}

	/**
	 * Full re-layout after the scroll offset changes - see the class doc. {@code clearWidgets} leaves the
	 * removed widget focused, so keystrokes would keep going to an invisible old text field; {@link #init}
	 * adds the same widgets in the same order every time, so focus moves to the new one at the same index.
	 */
	private void rebuild() {
		int focusedIndex = this.children().indexOf(this.getFocused());
		this.clearWidgets();
		this.setFocused(null);
		this.init();
		if (focusedIndex >= 0 && focusedIndex < this.children().size()) {
			this.setFocused(this.children().get(focusedIndex));
		}
	}

	private boolean isInViewport(double mouseY) {
		return mouseY >= TOP_MARGIN && mouseY < this.height - BOTTOM_MARGIN;
	}

	private CycleButton<Boolean> displayToggle(int x, int y, int width, String key, boolean initial, Consumer<Boolean> setter) {
		return CycleButton.onOffBuilder(initial)
				.create(x, y, width, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_edit." + key),
						(button, value) -> {
							setter.accept(value);
							ClientConfig.get().save();
						});
	}

	private EditBox coordField(int x, int y, int width, Component hint, int initial, IntConsumer onValid) {
		EditBox field = new EditBox(this.font, x, y, width, ROW_HEIGHT, hint);
		field.setHint(hint);
		field.setMaxLength(9);
		field.setValue(String.valueOf(initial));
		// EditBox#setFilter no longer exists - reject non-matching keystrokes by reverting to the
		// last matching value instead, same substitute pattern as ColorPickerPanel#setPatternFilteredResponder.
		String[] lastValid = {field.getValue()};
		field.setResponder(value -> {
			if (!value.matches("-?\\d{0,8}")) {
				field.setValue(lastValid[0]);
				return;
			}
			lastValid[0] = value;
			try {
				onValid.accept(Integer.parseInt(value));
				ClientConfig.get().save();
			} catch (NumberFormatException ignored) {
				// Mid-edit state (e.g. just "-" or empty) - keep the last valid value until a full number is typed.
			}
		});
		return field;
	}

	private void deleteWaypoint() {
		// Always the same world/server this waypoint was listed from - WaypointListScreen only ever
		// shows the current world's own waypoints, so re-deriving the key here still lands correctly.
		String worldKey = WaypointScope.currentKey(this.minecraft);
		if (worldKey != null) {
			ClientConfig.get().waypointsFor(worldKey).remove(this.waypoint);
			ClientConfig.get().save();
		}
		this.onClose();
	}

	/** Vanilla's own "are you sure?" dialog (same one world deletion etc. uses) - "No"/Escape returns here unchanged. */
	private void confirmDelete() {
		this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				this.deleteWaypoint();
			} else {
				this.minecraft.setScreen(this);
			}
		}, Component.translatable("gui.tntsallin1client.waypoint_edit.delete_confirm_title"),
				Component.translatable("gui.tntsallin1client.waypoint_edit.delete_confirm_message", this.waypoint.name)));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.centeredText(this.font,
				Component.translatable("gui.tntsallin1client.waypoint_edit.dimension", WaypointDimensions.label(this.waypoint.dimension)),
				this.width / 2, this.dimensionLabelY, 0xFFAAAAAA);
		if (this.colorPicker != null) {
			this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
		}
		guiGraphics.disableScissor();

		if (this.scrollBar != null) {
			this.scrollBar.render(guiGraphics);
		}
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.scrollBar != null && this.scrollBar.mouseClicked(event)) {
			return true;
		}
		// Widgets scrolled out of view are only clipped, not removed - don't let a click on the title
		// or the bottom margin hit one of them.
		if (!this.isInViewport(event.y())) {
			return false;
		}
		if (this.colorPicker != null && this.colorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar != null && this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		if (this.colorPicker != null && this.colorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean scrollBarReleased = this.scrollBar != null && this.scrollBar.mouseReleased();
		boolean pickerReleased = this.colorPicker != null && this.colorPicker.mouseReleased();
		if (scrollBarReleased || pickerReleased) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		if (this.maxScroll <= 0) {
			return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
		}
		int newOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollDeltaY * SCROLL_STEP), 0, this.maxScroll);
		if (newOffset != this.scrollOffset) {
			this.scrollOffset = newOffset;
			this.rebuild();
		}
		return true;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
