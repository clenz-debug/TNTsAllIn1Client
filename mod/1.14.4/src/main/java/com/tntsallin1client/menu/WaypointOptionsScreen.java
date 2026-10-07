package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * Options of the waypoints: on/off, the two keys (list, new waypoint), asking before a delete, the
 * arrows at the screen edge, what a new waypoint starts with, and the way to the list
 * ({@link WaypointListScreen}) - in the order the other versions have them.
 */
public class WaypointOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.waypoint_options.";

	public WaypointOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.waypointsEnabled, value -> config.waypointsEnabled = value);
		addKeyBinding(KEY + "key", ModKeyBindings.OPEN_WAYPOINTS);
		addKeyBinding(KEY + "create_key", ModKeyBindings.CREATE_WAYPOINT);
		addToggle(KEY + "confirm_delete", () -> config.waypointConfirmDelete, value -> config.waypointConfirmDelete = value);
		addToggle(KEY + "offscreen_arrows", () -> config.waypointOffscreenArrows, value -> config.waypointOffscreenArrows = value);

		// The four below only seed new waypoints - each waypoint has its own copy.
		addLabel(() -> I18n.get(KEY + "defaults_label"));
		addToggle(KEY + "show_beam", () -> config.waypointShowBeam, value -> config.waypointShowBeam = value);
		addToggle(KEY + "show_marker", () -> config.waypointShowMarker, value -> config.waypointShowMarker = value);
		addToggle(KEY + "show_distance", () -> config.waypointShowDistance, value -> config.waypointShowDistance = value);
		addToggle(KEY + "fade_nearby", () -> config.waypointFadeNearby, value -> config.waypointFadeNearby = value);

		addLink(KEY + "manage_button", () -> new WaypointListScreen(this));
		setResettable(ConfigReset.Feature.WAYPOINTS);
	}
}
