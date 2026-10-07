package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * Options of the Discord activity: which details get shown - singleplayer/multiplayer, the game
 * version, the world's name, the elapsed time - plus the one that gives something away (the
 * server's name), off unless switched on.
 */
public class DiscordPresenceOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.discord_presence_options.";

	public DiscordPresenceOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		// The activity is skipped in offline mode - said here, as the switches below still read "on".
		addLabel(() -> I18n.get(KEY + "offline_hint")).onlyIf(OfflineProfile::isOfflineLaunch);
		addToggle(KEY + "enabled", () -> config.discordPresenceEnabled, value -> config.discordPresenceEnabled = value);
		addToggle(KEY + "show_game_mode", () -> config.discordPresenceShowGameMode, value -> config.discordPresenceShowGameMode = value);
		addToggle(KEY + "show_version", () -> config.discordPresenceShowVersion, value -> config.discordPresenceShowVersion = value);
		addToggle(KEY + "show_world_name", () -> config.discordPresenceShowWorldName, value -> config.discordPresenceShowWorldName = value);
		addToggle(KEY + "show_server_name", () -> config.discordPresenceShowServerName, value -> config.discordPresenceShowServerName = value);
		addToggle(KEY + "show_elapsed_time", () -> config.discordPresenceShowElapsedTime, value -> config.discordPresenceShowElapsedTime = value);
		setResettable(ConfigReset.Feature.DISCORD_PRESENCE);
	}
}
