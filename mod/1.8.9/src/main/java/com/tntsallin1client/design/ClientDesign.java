package com.tntsallin1client.design;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Minecraft design vs. client design, as in the Fabric versions. Shared with the launcher through
 * `config/tntsallin1client-design.json`: the launcher writes its "Client-Design" setting there before
 * every launch and reads it back after the game exits (`clientDesignSync.ts`), so switching here ends
 * up in the launcher's settings too.
 */
public final class ClientDesign {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String FILE_NAME = "tntsallin1client-design.json";

	private static Boolean client;

	private ClientDesign() {
	}

	public static synchronized boolean isClient() {
		if (client == null) {
			client = load();
		}
		return client;
	}

	public static synchronized void setClient(boolean value) {
		client = value;
		JsonObject json = new JsonObject();
		json.addProperty("design", value ? "client" : "minecraft");
		File file = file();
		try {
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOGGER.warn("Failed to save client design to " + file, e);
		}
	}

	private static boolean load() {
		File file = file();
		if (!file.isFile()) {
			return false;
		}
		try {
			JsonObject json = new Gson().fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), JsonObject.class);
			return json != null && json.has("design") && "client".equals(json.get("design").getAsString());
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException | ClassCastException e) {
			LOGGER.warn("Failed to read client design from " + file, e);
			return false;
		}
	}

	private static File file() {
		return new File(new File(MinecraftClient.getInstance().runDirectory, "config"), FILE_NAME);
	}
}
