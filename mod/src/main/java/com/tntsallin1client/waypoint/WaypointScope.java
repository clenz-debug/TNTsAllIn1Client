package com.tntsallin1client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * Computes a stable key for "which world/server am I in right now", so waypoints can be kept
 * separate per save/server instead of one single global list (see {@link com.tntsallin1client.config.ClientConfig#waypointsByWorld}).
 *
 * <p>Singleplayer: keyed by the save folder name ({@code getWorldPath(LevelResource.ROOT)}'s last
 * path segment), not the in-game display name shown in the world list - the display name can be
 * freely renamed without touching the folder, the folder name can't (short of the player renaming
 * it on disk, same edge case vanilla itself accepts for e.g. options/resource-pack-per-world).
 *
 * <p>Multiplayer: keyed by {@link ServerData#ip} (the actual address, always unique) rather than
 * {@link ServerData#name} (an editable display label, not guaranteed unique or even present).
 * {@code getCurrentServer()} can be {@code null} (e.g. some direct-connect paths) - falls back to
 * a fixed "unknown" bucket, same fallback vanilla's own {@code Minecraft#archiveProfilingReport}
 * uses for the exact same "what's my current server called" question.
 *
 * <p>Returns {@code null} outside of any world (e.g. the title screen, where the mod menu is also
 * reachable per 5u) - callers must handle that case explicitly rather than falling back to some
 * shared bucket, so waypoints never silently leak between unrelated worlds/servers.
 */
public final class WaypointScope {
	private static final String SINGLEPLAYER_PREFIX = "sp:";
	private static final String MULTIPLAYER_PREFIX = "mp:";
	private static final String UNKNOWN_SERVER = "unknown";

	private WaypointScope() {
	}

	public static @Nullable String currentKey(Minecraft client) {
		if (client.isLocalServer() && client.getSingleplayerServer() != null) {
			// LevelResource.ROOT resolves to ".../<saveFolder>/." (a literal trailing "." path segment,
			// Path#resolve is purely syntactic) - getFileName() on that returns "." itself, not the save
			// folder, which made every singleplayer world collapse onto the same "sp:." key (confirmed
			// live: a second world showed the first world's waypoints). normalize() collapses the "."
			// away first, so getFileName() actually returns the save folder name - verified directly
			// against java.nio.file.Path's documented behavior, not just assumed.
			String saveFolder = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
			return SINGLEPLAYER_PREFIX + saveFolder;
		}

		if (client.level != null) {
			ServerData serverData = client.getCurrentServer();
			String address = serverData != null && serverData.ip != null ? serverData.ip : UNKNOWN_SERVER;
			return MULTIPLAYER_PREFIX + address;
		}

		return null;
	}
}
