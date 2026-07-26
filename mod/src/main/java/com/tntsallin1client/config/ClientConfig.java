package com.tntsallin1client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.hud.HudLayout;
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

	// 5a: which parts of the coordinates HUD to show - independent toggles, not
	// mutually exclusive (all three on is "show everything").
	public boolean coordinatesHudEnabled = true;
	public boolean coordinatesHudShowCoordinates = true;
	public boolean coordinatesHudShowDirection = true;
	public boolean coordinatesHudShowDegrees = true;
	public HudLayout coordinatesHudLayout = new HudLayout();

	// 5b: which item to tally across the inventory. Either a fixed item id, or
	// whatever is currently in the main hand.
	public boolean materialCounterEnabled = true;
	public boolean materialCounterUseHeldItem = false;
	public String materialCounterItemId = "minecraft:diamond";
	public HudLayout materialCounterHudLayout = new HudLayout();

	// 5c: quick-sort button + keybind in the player's own inventory screen.
	public boolean quickSortEnabled = true;

	// 5d: extra "Quick Info" block on the F3 debug screen.
	public boolean f3QuickInfoEnabled = true;

	// Phase 2 leftover: the "TNT's All-In-1 Client (Mixin active)" top-left label.
	public boolean clientNameLabelEnabled = true;

	// 5g: always-visible FPS counter, no F3 needed.
	public boolean fpsCounterEnabled = true;
	public HudLayout fpsCounterHudLayout = new HudLayout();

	// 5h: hold-to-zoom.
	public boolean zoomEnabled = true;

	// 5i: custom crosshair color. Defaults to white so turning this on doesn't
	// visibly change anything until the user actually picks a color.
	public boolean customCrosshairEnabled = true;
	public int customCrosshairColor = 0xFFFFFFFF;

	// 5j: fullbright (forced gamma override) and the always-visible light-level line.
	public boolean fullbrightEnabled = false;
	public boolean lightLevelHudEnabled = true;
	public HudLayout lightLevelHudLayout = new HudLayout();

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
