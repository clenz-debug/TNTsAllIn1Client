package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Waypoint idea: opens {@link WaypointListScreen} from gameplay via keybind - same
 * "only while no other screen is open" gate {@link PauseMenuIntegration} uses for
 * {@link ModKeyBindings#OPEN_MENU}. Gated on {@link ClientConfig#waypointsEnabled} like every
 * other feature's own keybind here (e.g. zoom, spawn overlay) - turning the feature off in the
 * mod menu turns its key off too.
 */
public final class WaypointMenuIntegration {
	private WaypointMenuIntegration() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ModKeyBindings.OPEN_WAYPOINTS.consumeClick()) {
				if (client.screen == null && ClientConfig.get().waypointsEnabled) {
					client.setScreen(new WaypointListScreen(null));
				}
			}
		});
	}
}
