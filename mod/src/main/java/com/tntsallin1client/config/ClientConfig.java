package com.tntsallin1client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-backed config so each Phase 5 feature can be toggled independently
 * without a settings screen yet (that lands with the ingame menu, 5e).
 */
public class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client.json");

	private static ClientConfig instance;

	public boolean coordinatesHudEnabled = true;

	public static ClientConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static ClientConfig load() {
		if (Files.exists(FILE)) {
			try (BufferedReader reader = Files.newBufferedReader(FILE)) {
				ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException e) {
				TNTsAllIn1ClientMod.LOGGER.warn("[{}] Failed to read config, falling back to defaults.", TNTsAllIn1ClientMod.MOD_ID, e);
			}
		}

		ClientConfig defaults = new ClientConfig();
		defaults.save();
		return defaults;
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			TNTsAllIn1ClientMod.LOGGER.warn("[{}] Failed to save config.", TNTsAllIn1ClientMod.MOD_ID, e);
		}
	}
}
