package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * Waypoint idea: edit a single waypoint (name, position, color, visibility) or delete it.
 * {@link #waypoint} is the live object inside {@link ClientConfig#waypoints}, not a copy - every
 * field responder writes straight back into it and saves immediately, same pattern as every other
 * settings screen in this mod. Dimension is shown but not editable (always set from where the
 * waypoint was created) - keeps this screen from needing a whole dimension picker for what would
 * be a rare edit; recreating the waypoint while standing in the target dimension covers that case.
 */
public class WaypointEditScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int FIELD_GAP = 6;

	private final Screen parent;
	private final Waypoint waypoint;
	private @Nullable ColorPickerPanel colorPicker;
	private int dimensionLabelY;

	public WaypointEditScreen(Screen parent, Waypoint waypoint) {
		super(Component.translatable("gui.tntsallin1client.waypoint_edit.title"));
		this.parent = parent;
		this.waypoint = waypoint;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 36;

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

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.waypoint_edit.delete_button"),
						button -> {
							if (config.waypointConfirmDelete) {
								this.confirmDelete();
							} else {
								this.deleteWaypoint();
							}
						})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_HEIGHT + FIELD_GAP;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private EditBox coordField(int x, int y, int width, Component hint, int initial, IntConsumer onValid) {
		EditBox field = new EditBox(this.font, x, y, width, ROW_HEIGHT, hint);
		field.setHint(hint);
		field.setMaxLength(9);
		field.setFilter(value -> value.matches("-?\\d{0,8}"));
		field.setValue(String.valueOf(initial));
		field.setResponder(value -> {
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		guiGraphics.drawCenteredString(this.font,
				Component.translatable("gui.tntsallin1client.waypoint_edit.dimension", WaypointDimensions.label(this.waypoint.dimension)),
				this.width / 2, this.dimensionLabelY, 0xFFAAAAAA);
		if (this.colorPicker != null) {
			this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.colorPicker != null && this.colorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.colorPicker != null && this.colorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.colorPicker != null && this.colorPicker.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
