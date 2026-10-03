package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.MinecraftClient;

/**
 * The waypoints' two keys: one opens the list, one goes straight to "New Waypoint". Both only from
 * gameplay - with a screen open a key belongs to that screen - and only while waypoints are
 * switched on: switching the feature off in the mod menu switches its keys off too.
 */
public final class WaypointMenuIntegration {
	private WaypointMenuIntegration() {
	}

	/** Called every game tick. */
	public static void tick(MinecraftClient client) {
		while (ModKeyBindings.OPEN_WAYPOINTS.wasPressed()) {
			if (client.currentScreen == null && ClientConfig.get().waypointsEnabled) {
				client.setScreen(new WaypointListScreen(null));
			}
		}
		while (ModKeyBindings.CREATE_WAYPOINT.wasPressed()) {
			if (client.currentScreen == null && ClientConfig.get().waypointsEnabled) {
				client.setScreen(new WaypointCreateScreen(null));
			}
		}
	}
}
