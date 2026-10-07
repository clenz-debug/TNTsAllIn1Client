package com.tntsallin1client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Names the world or server the player is in right now, so every one of them has its own waypoints
 * (see {@code ClientConfig#waypointsByWorld}) - the same keys as in the other versions of the mod.
 *
 * <p>Singleplayer: the save's folder name, not the name shown in the world list - that one can be
 * changed at any time, the folder stays. Multiplayer: the server's address, not its name in the
 * server list. A connection the game has no server entry for goes into one shared "unknown" list.
 */
public final class WaypointScope {
	private static final String SINGLEPLAYER_PREFIX = "sp:";
	private static final String MULTIPLAYER_PREFIX = "mp:";
	private static final String UNKNOWN_SERVER = "unknown";

	private WaypointScope() {
	}

	/** @return null outside of a world (the mod menu is reachable from the title screen too) */
	public static String currentKey(Minecraft client) {
		if (client.level == null) {
			return null;
		}
		if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
			// The folder name: the integrated server is created with (folder, display name) and keeps
			// the first as its level id.
			return SINGLEPLAYER_PREFIX + client.getSingleplayerServer().getLevelIdName();
		}
		ServerData server = client.getCurrentServer();
		return MULTIPLAYER_PREFIX + (server != null && server.ip != null ? server.ip : UNKNOWN_SERVER);
	}
}
