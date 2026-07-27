package com.tntsallin1client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.crosshair.CrosshairGrid;
import com.tntsallin1client.crosshair.CrosshairMode;
import com.tntsallin1client.crosshair.CrosshairPreset;
import com.tntsallin1client.hud.HudLayout;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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
	// Scroll-adjustable while zooming (see ZoomHandler); persists as the
	// "remembered" zoom level between sessions, same as every other setting here.
	public int zoomFov = 15;

	// 5i redesigned: custom crosshair color (defaults to white so turning this
	// on doesn't visibly change anything until a color is picked), a shape
	// (preset library or user-drawn 9x9 grid), a size independent of GUI Scale,
	// and an optional second color while aiming at an attackable mob.
	public boolean customCrosshairEnabled = true;
	public int customCrosshairColor = 0xFFFFFFFF;
	public CrosshairMode crosshairMode = CrosshairMode.PRESET;
	public CrosshairPreset crosshairPreset = CrosshairPreset.SMALLER;
	public boolean[][] crosshairCustomGrid = CrosshairGrid.empty();
	public int crosshairPixelSize = 2;
	public boolean crosshairIgnoreGuiScale = false;
	public boolean crosshairTargetColorEnabled = false;
	public int crosshairTargetColor = 0xFFFF5555;

	// 5j: fullbright (forced gamma override).
	public boolean fullbrightEnabled = false;

	// 5j redesigned: was a plain "Light: N" text HUD line; replaced by a
	// key-triggered in-world overlay marking nearby mob-spawnable positions
	// with colored X marks (see SpawnOverlayRenderer). Hold-vs-toggle is a
	// separate setting since either can be the more comfortable one depending
	// on how someone plays.
	public boolean spawnOverlayEnabled = true;
	public boolean spawnOverlayHoldMode = true;

	// 5k: hold-to-preview shulker box contents beyond vanilla's own 5-item cap.
	public boolean shulkerPreviewEnabled = true;

	// 5l: F3+B hitbox outline color. Defaults to white, vanilla's own color, so
	// turning this on doesn't visibly change anything until a color is picked.
	public boolean customHitboxColorEnabled = true;
	public int customHitboxColor = 0xFFFFFFFF;

	// 5q: the black wireframe box drawn around whatever block is being looked
	// at. Same "alpha byte never shown as-is" pattern as keystrokesActiveColor -
	// vanilla's own alpha (translucent normally, opaque in high-contrast mode)
	// is always kept, only the RGB comes from here. Defaults to black, vanilla's
	// own color, so turning this on doesn't visibly change anything until picked.
	public boolean customBlockOutlineColorEnabled = true;
	public int customBlockOutlineColor = 0xFF000000;

	// 5m: keystrokes overlay (WASD/Shift/Space/mouse buttons + sprint/drop).
	// Active-box color, same green as the original hardcoded default; the
	// alpha byte here is never actually shown as-is (KeystrokesHud always
	// re-applies its own translucent glow alpha on top), only the RGB matters.
	public boolean keystrokesEnabled = true;
	public HudLayout keystrokesHudLayout = new HudLayout();
	public int keystrokesActiveColor = 0xFF33CC33;
	// Per-key on/off, keyed by KeystrokeKey#name() - a key absent from the map
	// (the common case, nothing has been toggled off yet) counts as enabled.
	public Map<String, Boolean> keystrokesKeyEnabled = new HashMap<>();

	// 5n: Open/Copy popup after taking a screenshot.
	public boolean screenshotToastEnabled = true;

	// 5o: cosmetic dropped-item lean/tilt ("item physics" light).
	public boolean itemTiltEnabled = true;

	// 5r: darkens the light gray inventory/container background panel (chest,
	// player inventory, furnace, crafting table, ...). Off by default, unlike
	// the color-picker features above - unlike a picker that starts at "looks
	// like vanilla", this changes the look the moment it's turned on, so it
	// should be an explicit opt-in rather than a surprise.
	public boolean darkInventoryEnabled = false;

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
			} catch (IOException | JsonParseException e) {
				// JsonParseException (e.g. a saved field that no longer matches its
				// current Java type - confirmed the hard way: crosshairPixelSize
				// briefly went from int to float and back, leaving a fractional
				// value on disk that GSON couldn't parse back into an int) used to
				// propagate straight out of here uncaught, crashing the whole game
				// at startup instead of just falling back to defaults like a bad
				// config file always should.
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
