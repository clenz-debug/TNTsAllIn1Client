package com.tntsallin1client.discord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shows in the player's Discord status that they play through this client, with as much detail as
 * they switched on: singleplayer/multiplayer, the game version, the world's or the server's name,
 * the time spent there. Looked at about once a second from the game's tick.
 *
 * <p>Everything that talks to Discord runs on its own thread, never on the game's: Discord only
 * answers the first hello once it has reached its own servers, so without internet that read never
 * returns - on the game's thread the game would hang with it. The game's thread only decides what
 * to send and hands it over; while a job is still running it simply skips.
 */
public final class DiscordPresenceManager {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	/** Our own Discord Application - Discord shows its name and logo. The same one as in the other versions; public by nature. */
	private static final long APPLICATION_ID = 1551954462359429140L;
	private static final long RECONNECT_INTERVAL_MILLIS = 15_000;
	private static final int TICKS_BETWEEN_UPDATES = 20;
	private static final String MINECRAFT_VERSION = "1.14.4";

	private static final ExecutorService IPC = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "TNT Discord IPC");
		thread.setDaemon(true);
		return thread;
	});

	/** Only ever written on the {@link #IPC} thread, read on the game's. */
	private static volatile DiscordIpcClient client;
	private static volatile RichPresenceData lastSent;
	private static volatile String lastScopeKey;
	/** A job is waiting or running on {@link #IPC} - no new one until it is done. */
	private static volatile boolean busy;
	private static long lastConnectAttemptMillis;
	private static long scopeStartMillis;
	private static int tickCounter;

	private DiscordPresenceManager() {
	}

	public static void tick(Minecraft mc) {
		if (busy) {
			return;
		}
		ClientConfig config = ClientConfig.get();
		// Without internet Discord can't show anything anyway.
		if (!config.discordPresenceEnabled || OfflineProfile.isOfflineLaunch()) {
			if (client != null) {
				runOnIpc(DiscordPresenceManager::disconnect);
			}
			return;
		}

		if (++tickCounter < TICKS_BETWEEN_UPDATES) {
			return;
		}
		tickCounter = 0;

		if (client == null) {
			maybeReconnect();
			return;
		}

		final RichPresenceData desired = computePresence(mc, config);
		if (!desired.equals(lastSent)) {
			runOnIpc(() -> send(desired));
		}
	}

	private static void runOnIpc(final Runnable job) {
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
		if (current == null) {
			return;
		}
		try {
			current.setActivity(desired);
			lastSent = desired;
		} catch (IOException e) {
			LOGGER.info("Discord connection lost, will retry.");
			disconnect();
		}
	}

	private static void maybeReconnect() {
		long now = System.currentTimeMillis();
		if (now - lastConnectAttemptMillis < RECONNECT_INTERVAL_MILLIS) {
			return;
		}
		lastConnectAttemptMillis = now;
		runOnIpc(() -> {
			try {
				client = DiscordIpcClient.connect(APPLICATION_ID);
			} catch (IOException e) {
				// Discord isn't running - not worth a log line every 15 seconds.
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
		Long start = config.discordPresenceShowElapsedTime ? Long.valueOf(scopeStartMillis) : null;

		if (mc.level == null) {
			return new RichPresenceData(I18n.get("gui.tntsallin1client.discord_presence.main_menu"), null, start);
		}

		boolean singleplayer = isSingleplayer(mc);

		// The two lines Discord has are filled in this order with whatever is switched on (and fits
		// singleplayer or multiplayer): version, game mode, then the world's or the server's name.
		List<String> parts = new ArrayList<String>();
		if (config.discordPresenceShowVersion) {
			parts.add(I18n.get("gui.tntsallin1client.discord_presence.version", MINECRAFT_VERSION));
		}
		if (config.discordPresenceShowGameMode) {
			parts.add(I18n.get(singleplayer
					? "gui.tntsallin1client.discord_presence.singleplayer"
					: "gui.tntsallin1client.discord_presence.multiplayer"));
		}
		if (singleplayer && config.discordPresenceShowWorldName) {
			parts.add(mc.getSingleplayerServer().getLevelName());
		} else if (!singleplayer && config.discordPresenceShowServerName) {
			String name = serverName(mc);
			if (name != null) {
				parts.add(name);
			}
		}

		String details = parts.isEmpty() ? I18n.get("gui.tntsallin1client.discord_presence.playing_generic") : parts.get(0);
		String state = parts.size() > 1 ? String.join(" – ", parts.subList(1, parts.size())) : null;
		return new RichPresenceData(details, state, start);
	}

	private static boolean isSingleplayer(Minecraft mc) {
		return mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null;
	}

	/** Which place the player is in - the elapsed time starts over when it changes. The same naming of worlds and servers as the waypoints use. */
	private static String scopeKey(Minecraft mc) {
		if (mc.level == null) {
			return "menu";
		}
		if (isSingleplayer(mc)) {
			return "sp:" + mc.getSingleplayerServer().getLevelName();
		}
		ServerData server = mc.getCurrentServer();
		return "mp:" + (server != null && server.ip != null ? server.ip : "unknown");
	}

	private static String serverName(Minecraft mc) {
		ServerData server = mc.getCurrentServer();
		if (server == null) {
			return null;
		}
		return server.name != null && !server.name.trim().isEmpty() ? server.name : server.ip;
	}
}
