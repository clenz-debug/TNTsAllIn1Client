package com.tntsallin1client.discord;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.offline.OfflineProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drives Discord Rich Presence off the client's own tick loop - own user request ("in Discord soll
 * angezeigt werden das man MC über meinen Client spielt", extended on request to be fully
 * configurable: master on/off plus independent toggles for game mode, world name, version, server
 * name). Ticked from {@link com.tntsallin1client.TNTsAllIn1ClientMod#onInitializeClient}, throttled
 * to roughly once a second - state here changes at human timescales (menu/world transitions, a
 * toggle flipped in the options screen), never worth recomputing 20x/second.
 *
 * <p>Singleplayer/multiplayer detection and the singleplayer world's identity both reuse the exact
 * API {@link com.tntsallin1client.waypoint.WaypointScope} already uses and comments extensively on
 * (why {@code getWorldPath(LevelResource.ROOT)} needs {@code normalize()} before
 * {@code getFileName()}, why multiplayer is keyed by {@code ServerData#ip} not the editable
 * {@code #name}) - same underlying "which world/server am I in" question, so the same proven answer.
 *
 * <p>All IPC (connect, handshake, sending) runs on its own {@link #IPC} thread, never on the tick
 * thread: Discord only answers the handshake once it's connected to its own servers, so without
 * internet the handshake read blocks indefinitely - on the tick thread that froze the whole game at
 * the loading screen (found in the offline mode's first live test). The tick thread only decides
 * what to send and hands it over; while a job is still {@link #busy}, it simply skips.
 */
public final class DiscordPresenceManager {
	/**
	 * Our own Discord Application (discord.com/developers/applications, the client's logo uploaded
	 * there as "logo") - Discord shows its name and logo for the presence. Public by nature: Discord
	 * hands it to everyone who sees the presence. It used to be a config field defaulting to a
	 * placeholder and was only ever filled in by hand in one dev instance, so every instance the
	 * installed launcher created quietly never connected (own user report after 0.1.0).
	 */
	private static final long APPLICATION_ID = 1551954462359429140L;
	private static final long RECONNECT_INTERVAL_MILLIS = 15_000;
	private static final int TICKS_BETWEEN_UPDATES = 20;

	private static final ExecutorService IPC = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "TNT Discord IPC");
		thread.setDaemon(true);
		return thread;
	});

	/** Only ever written on the {@link #IPC} thread, read on the tick thread. */
	private static volatile @Nullable DiscordIpcClient client;
	private static volatile @Nullable RichPresenceData lastSent;
	private static volatile @Nullable String lastScopeKey;
	/** A job is queued or running on {@link #IPC} - no new one until it's done. */
	private static volatile boolean busy;
	private static long lastConnectAttemptMillis = 0;
	private static long scopeStartMillis;
	private static int tickCounter;

	private DiscordPresenceManager() {
	}

	public static void tick(Minecraft mc) {
		if (busy) return;
		ClientConfig config = ClientConfig.get();
		// Offline launch (see OfflineProfile): Discord can't show a presence without internet anyway.
		if (!config.discordPresenceEnabled || OfflineProfile.isOfflineLaunch()) {
			if (client != null) runOnIpc(DiscordPresenceManager::disconnect);
			return;
		}

		if (++tickCounter < TICKS_BETWEEN_UPDATES) return;
		tickCounter = 0;

		if (client == null) {
			maybeReconnect();
			return;
		}

		RichPresenceData desired = computePresence(mc, config);
		if (!desired.equals(lastSent)) {
			runOnIpc(() -> send(desired));
		}
	}

	private static void runOnIpc(Runnable job) {
		busy = true;
		IPC.execute(() -> {
			try {
				job.run();
			} finally {
				busy = false;
			}
		});
	}

	/** On the {@link #IPC} thread. */
	private static void send(RichPresenceData desired) {
		DiscordIpcClient current = client;
		if (current == null) return;
		try {
			current.setActivity(desired);
			lastSent = desired;
		} catch (IOException e) {
			TNTsAllIn1ClientMod.LOGGER.info("[{}] Discord IPC connection lost, will retry.", TNTsAllIn1ClientMod.MOD_ID);
			disconnect();
		}
	}

	private static void maybeReconnect() {
		long now = System.currentTimeMillis();
		if (now - lastConnectAttemptMillis < RECONNECT_INTERVAL_MILLIS) return;
		lastConnectAttemptMillis = now;
		runOnIpc(() -> {
			try {
				client = DiscordIpcClient.connect(APPLICATION_ID);
			} catch (IOException e) {
				// Discord not running (every candidate pipe/socket connect failed) - not worth logging
				// every 15s, simply "not ready yet" until it is.
				client = null;
			}
		});
	}

	/** On the {@link #IPC} thread. */
	private static void disconnect() {
		DiscordIpcClient current = client;
		if (current != null) {
			current.close();
			client = null;
		}
		lastSent = null;
		lastScopeKey = null;
	}

	private static RichPresenceData computePresence(Minecraft mc, ClientConfig config) {
		String scopeKey = scopeKey(mc);
		if (!scopeKey.equals(lastScopeKey)) {
			lastScopeKey = scopeKey;
			scopeStartMillis = System.currentTimeMillis();
		}
		Long start = config.discordPresenceShowElapsedTime ? scopeStartMillis : null;

		if (mc.level == null) {
			return new RichPresenceData(translated("gui.tntsallin1client.discord_presence.main_menu"), null, start);
		}

		boolean singleplayer = mc.isLocalServer() && mc.getSingleplayerServer() != null;

		// Priority order for the two available text lines: version, then game mode, then
		// world/server name - whichever of these are actually toggled on (and applicable to the
		// current SP/MP state) fill `details` first, then `state`; a subset of one line is fine,
		// Discord just shows what's there.
		List<String> parts = new ArrayList<>();
		if (config.discordPresenceShowVersion) {
			parts.add(translated("gui.tntsallin1client.discord_presence.version", currentMinecraftVersion()));
		}
		if (config.discordPresenceShowGameMode) {
			parts.add(translated(singleplayer
					? "gui.tntsallin1client.discord_presence.singleplayer"
					: "gui.tntsallin1client.discord_presence.multiplayer"));
		}
		if (singleplayer && config.discordPresenceShowWorldName) {
			parts.add(singleplayerWorldName(mc));
		} else if (!singleplayer && config.discordPresenceShowServerName) {
			String name = serverName(mc);
			if (name != null) parts.add(name);
		}

		String details = parts.isEmpty() ? translated("gui.tntsallin1client.discord_presence.playing_generic") : parts.get(0);
		String state = parts.size() > 1 ? String.join(" – ", parts.subList(1, parts.size())) : null;
		return new RichPresenceData(details, state, start);
	}

	/** Same scope identity as {@link com.tntsallin1client.waypoint.WaypointScope#currentKey} - not
	 * reused directly (that one returns {@code null} outside a world, this needs a distinct "menu"
	 * key instead so the elapsed timer still resets on entering/leaving the menu). */
	private static String scopeKey(Minecraft mc) {
		if (mc.level == null) return "menu";
		if (mc.isLocalServer() && mc.getSingleplayerServer() != null) {
			return "sp:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getFileName();
		}
		ServerData server = mc.getCurrentServer();
		return "mp:" + (server != null && server.ip != null ? server.ip : "unknown");
	}

	private static String singleplayerWorldName(Minecraft mc) {
		return mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
	}

	private static @Nullable String serverName(Minecraft mc) {
		ServerData server = mc.getCurrentServer();
		if (server == null) return null;
		return server.name != null && !server.name.isBlank() ? server.name : server.ip;
	}

	/** Loader-level, not Minecraft's own (obfuscated/mapped) version API - stable across MC versions
	 * on purpose, same reasoning `build.gradle.kts`'s own `minecraft_version` project property exists
	 * for, just read live at runtime instead of baked in at build time. */
	private static String currentMinecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("?");
	}

	/** Discord's activity fields are plain strings sent over IPC, not something a {@code Component}
	 * can render - so unlike every other user-facing string in this mod, these can't go through
	 * {@code Component.translatable(key)} rendered straight into a widget. Resolving them to a plain
	 * string via {@code .getString()} still routes through the same {@code lang/*.json} files and the
	 * player's actual active language, same translation pipeline, just consumed differently. */
	private static String translated(String key, Object... args) {
		return Component.translatable(key, args).getString();
	}
}
