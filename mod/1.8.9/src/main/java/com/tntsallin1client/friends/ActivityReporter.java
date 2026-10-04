package com.tntsallin1client.friends;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tells the launcher where in the game the player is, for what their friends see: main menu,
 * singleplayer, or which server. The launcher reads {@link #FILE} while the game runs and passes it
 * on - the server only if the player doesn't hide it there. Written only when it changes, checked
 * about once a second. Same file and content as in the Fabric versions.
 */
public final class ActivityReporter {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String FILE = "tntsallin1client-activity.json";
	private static final int TICKS_BETWEEN_CHECKS = 20;

	private static int tickCounter;
	private static String lastWritten;

	private ActivityReporter() {
	}

	public static void tick(MinecraftClient client) {
		if (++tickCounter < TICKS_BETWEEN_CHECKS) {
			return;
		}
		tickCounter = 0;

		JsonObject json = new JsonObject();
		if (client.world == null) {
			json.addProperty("kind", "menu");
		} else if (client.isIntegratedServerRunning()) {
			json.addProperty("kind", "singleplayer");
		} else {
			json.addProperty("kind", "multiplayer");
			ServerInfo server = client.getCurrentServerEntry();
			if (server != null && server.address != null && !server.address.trim().isEmpty()) {
				// The address, not the player's own label for it - that is what friends recognize.
				json.addProperty("server", server.address);
			}
		}

		String serialized = new Gson().toJson(json);
		if (serialized.equals(lastWritten)) {
			return;
		}
		File configDir = new File(client.runDirectory, "config");
		try {
			configDir.mkdirs();
			Files.write(new File(configDir, FILE).toPath(), serialized.getBytes(StandardCharsets.UTF_8));
			lastWritten = serialized;
		} catch (IOException e) {
			LOGGER.warn("Failed to write the activity for the launcher.", e);
		}
	}
}
