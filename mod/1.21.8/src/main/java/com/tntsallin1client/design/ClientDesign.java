package com.tntsallin1client.design;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.compat.EssentialCompat;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minecraft design vs. client design for the title screen and the Client Mods menu (own user
 * request). Shared with the launcher through {@link #FILE}: the launcher writes its "Client-Design"
 * setting there before every launch and reads it back after the game exits
 * ({@code clientDesignSync.ts}), so switching via the title screen's logo button here ends up in the
 * launcher's settings too.
 */
public final class ClientDesign {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-design.json");

	private static Boolean client;

	private ClientDesign() {
	}

	/** Always false while Essential is installed (see {@link EssentialCompat}) - without touching the saved choice, so it's back once Essential is removed. */
	public static synchronized boolean isClient() {
		if (EssentialCompat.isLoaded()) {
			return false;
		}
		if (client == null) {
			client = load();
		}
		return client;
	}

	public static synchronized void setClient(boolean value) {
		client = value;
		JsonObject json = new JsonObject();
		json.addProperty("design", value ? "client" : "minecraft");
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, new Gson().toJson(json));
		} catch (IOException e) {
			LOGGER.warn("Failed to save client design to {}", FILE, e);
		}
	}

	private static boolean load() {
		if (!Files.exists(FILE)) {
			return false;
		}
		try {
			JsonObject json = new Gson().fromJson(Files.readString(FILE), JsonObject.class);
			return json != null && json.has("design") && "client".equals(json.get("design").getAsString());
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
			LOGGER.warn("Failed to read client design from {}", FILE, e);
			return false;
		}
	}
}
