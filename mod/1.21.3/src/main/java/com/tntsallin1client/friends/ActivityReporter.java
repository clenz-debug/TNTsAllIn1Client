package com.tntsallin1client.friends;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Tells the launcher where in the game the player is, for the friends presence (Phase 8): main
 * menu, singleplayer, or which server. The launcher reads {@link #FILE} while the game runs
 * ({@code gameActivity.ts}) and reports it to friends, who only see the server if the player
 * doesn't hide it there. Written only when it changes, checked about once a second.
 */
public final class ActivityReporter {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-activity.json");
	private static final int TICKS_BETWEEN_CHECKS = 20;

	private static int tickCounter;
	private static String lastWritten;

	private ActivityReporter() {
	}

	public static void tick(Minecraft mc) {
		if (++tickCounter < TICKS_BETWEEN_CHECKS) return;
		tickCounter = 0;

		JsonObject json = new JsonObject();
		if (mc.level == null) {
			json.addProperty("kind", "menu");
		} else if (mc.isLocalServer()) {
			json.addProperty("kind", "singleplayer");
		} else {
			json.addProperty("kind", "multiplayer");
			ServerData server = mc.getCurrentServer();
			if (server != null && server.ip != null && !server.ip.isBlank()) {
				// The address, not the player's own label for it - that's what friends recognize.
				json.addProperty("server", server.ip);
			}
		}

		String serialized = new Gson().toJson(json);
		if (Objects.equals(serialized, lastWritten)) return;
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, serialized);
			lastWritten = serialized;
		} catch (IOException e) {
			LOGGER.warn("Failed to write activity to {}", FILE, e);
		}
	}
}
