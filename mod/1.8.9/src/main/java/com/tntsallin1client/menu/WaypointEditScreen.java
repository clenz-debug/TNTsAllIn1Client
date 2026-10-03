package com.tntsallin1client.menu;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

/**
 * One waypoint's own screen: name, position, color, visible or not, beam/marker/distance/fading -
 * and deleting it. Works on the waypoint in the config's list itself, every change is saved right
 * away.
 *
 * <p>The dimension is shown but can't be changed - it is where the waypoint was set. To move a
 * waypoint to another dimension, set a new one there.
 */
public class WaypointEditScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.waypoint_edit.";
	private static final int MAX_NAME_LENGTH = 24;
	/** A sign and eight digits. */
	private static final int MAX_COORDINATE_LENGTH = 9;

	private final Waypoint waypoint;

	public WaypointEditScreen(Screen parent, final Waypoint waypoint) {
		super(parent, KEY + "title");
		this.waypoint = waypoint;

		addTextField(KEY + "name", MAX_NAME_LENGTH, () -> waypoint.name, value -> waypoint.name = value, value -> true);
		addPanel(new SplitPanel(
				coordinateField("x", () -> waypoint.x, value -> waypoint.x = value),
				coordinateField("y", () -> waypoint.y, value -> waypoint.y = value),
				coordinateField("z", () -> waypoint.z, value -> waypoint.z = value)));
		addLabel(() -> I18n.translate(KEY + "dimension", WaypointDimensions.label(waypoint.dimension)));
		addColor(() -> waypoint.color, argb -> waypoint.color = argb);
		addToggle(KEY + "visible", () -> waypoint.visible, value -> waypoint.visible = value);
		// Two per row, as in the Fabric versions.
		addTogglePair(KEY + "show_beam", () -> waypoint.showBeam, value -> waypoint.showBeam = value,
				KEY + "show_marker", () -> waypoint.showMarker, value -> waypoint.showMarker = value);
		addTogglePair(KEY + "show_distance", () -> waypoint.showDistance, value -> waypoint.showDistance = value,
				KEY + "fade_nearby", () -> waypoint.fadeNearby, value -> waypoint.fadeNearby = value);
		// "Delete" left of "Back", as in the Fabric versions.
		setAction(KEY + "delete_button", this::delete, true);
	}

	/** A field for one coordinate. Anything that isn't a whole number shows in red and leaves the coordinate as it was. */
	private static TextFieldPanel coordinateField(String axis, final IntSupplier getter, final IntConsumer setter) {
		return new TextFieldPanel(KEY + axis, MAX_COORDINATE_LENGTH, () -> String.valueOf(getter.getAsInt()), value -> {
			if (isCoordinate(value)) {
				setter.accept(Integer.parseInt(value));
			}
		}, WaypointEditScreen::isCoordinate);
	}

	private static boolean isCoordinate(String text) {
		return text.matches("-?[0-9]{1,8}");
	}

	private void delete() {
		if (ClientConfig.get().waypointConfirmDelete) {
			// Asks first; the answer comes back through confirmResult.
			this.client.setScreen(new ConfirmScreen(this, I18n.translate(KEY + "delete_confirm_title"),
					I18n.translate(KEY + "delete_confirm_message", this.waypoint.name), 0));
		} else {
			deleteWaypoint();
		}
	}

	@Override
	public void confirmResult(boolean confirmed, int id) {
		if (confirmed) {
			deleteWaypoint();
		} else {
			this.client.setScreen(this);
		}
	}

	private void deleteWaypoint() {
		// The list this screen was opened from only shows the current world's waypoints.
		String worldKey = WaypointScope.currentKey(this.client);
		if (worldKey != null) {
			ClientConfig config = ClientConfig.get();
			config.waypointsFor(worldKey).remove(this.waypoint);
			config.save();
		}
		back();
	}
}
