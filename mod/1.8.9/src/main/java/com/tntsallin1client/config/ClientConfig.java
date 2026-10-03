package com.tntsallin1client.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.tntsallin1client.crosshair.CrosshairGrid;
import com.tntsallin1client.crosshair.CrosshairMode;
import com.tntsallin1client.crosshair.CrosshairPreset;
import com.tntsallin1client.hud.ArmorStatusColorMode;
import com.tntsallin1client.hud.ArmorStatusDirection;
import com.tntsallin1client.hud.ArmorStatusIconPosition;
import com.tntsallin1client.hud.ArmorStatusLayoutMode;
import com.tntsallin1client.hud.ArmorStatusSlot;
import com.tntsallin1client.hud.HudLayout;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JSON-backed config, one switch (and later its settings) per feature - same shape and file name as
 * in the Fabric versions of the mod. Every feature is off until switched on in the mod menu.
 */
public class ClientConfig {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static ClientConfig instance;

	// Colors are ARGB with full alpha, as the color picker hands them out. A HudLayout is where the
	// HUD editor put an element (see HudLayout).

	// Which parts of the coordinates HUD to show - independent switches, not mutually exclusive.
	public boolean coordinatesHudEnabled = false;
	public boolean coordinatesHudShowCoordinates = true;
	public boolean coordinatesHudShowDirection = true;
	public boolean coordinatesHudShowDegrees = true;
	public int coordinatesHudTextColor = 0xFFFFFFFF;
	public HudLayout coordinatesHudLayout = new HudLayout();

	// Total count of one item across the inventory - a fixed item id, or whatever is currently held.
	public boolean itemCounterEnabled = false;
	public boolean itemCounterUseHeldItem = false;
	public String itemCounterItemId = "minecraft:diamond";
	public boolean itemCounterShowItemIcon = false;
	public int itemCounterTextColor = 0xFFFFFFFF;
	public HudLayout itemCounterHudLayout = new HudLayout();

	// Always-visible FPS counter, no F3 needed.
	public boolean fpsCounterEnabled = false;
	public int fpsCounterTextColor = 0xFFFFFFFF;
	public HudLayout fpsCounterHudLayout = new HudLayout();

	// Always-visible latency (ping) in ms.
	public boolean latencyHudEnabled = false;
	public int latencyTextColor = 0xFFFFFFFF;
	public HudLayout latencyHudLayout = new HudLayout();

	// Always-visible clock - the real time of day, as "00:00" (24-hour) or with AM/PM (12-hour).
	public boolean clockHudEnabled = false;
	public boolean clockHud24Hour = true;
	public int clockTextColor = 0xFFFFFFFF;
	public HudLayout clockHudLayout = new HudLayout();

	// Keystrokes overlay (WASD/Shift/Space/mouse buttons + sprint/drop). Of the active-box color only
	// the RGB matters - KeystrokesHud applies its own see-through alpha.
	public boolean keystrokesEnabled = false;
	public int keystrokesActiveColor = 0xFF000000;
	public int keystrokesTextColor = 0xFFFFFFFF;
	// Per-key on/off, keyed by KeystrokeKey#name() - a key absent from the map (the common case,
	// nothing has been switched off yet) counts as shown.
	public Map<String, Boolean> keystrokesKeyEnabled = new HashMap<String, Boolean>();
	public HudLayout keystrokesHudLayout = new HudLayout();

	// Armor & Tool Status: durability of the four worn armor pieces and of the held item, or - for
	// stackable items like blocks - the count in that one stack. Each slot can be switched off
	// (armorStatusSlotEnabled, absent = shown) and put in another place in the order
	// (armorStatusSlotOrder, names from ArmorStatusSlot; whatever is missing follows in its natural
	// order). Shown as one bundled HUD element or as one element per slot - see ArmorStatusHud.
	public boolean armorStatusEnabled = false;
	public Map<String, Boolean> armorStatusSlotEnabled = new HashMap<String, Boolean>();
	public List<String> armorStatusSlotOrder = new ArrayList<String>();
	public boolean armorStatusShowName = true;
	public boolean armorStatusShowIcon = true;
	public ArmorStatusIconPosition armorStatusIconPosition = ArmorStatusIconPosition.LEFT;
	// Nudges the text up (negative) or down (positive) against the icon - the font leaves blank
	// space below digits, so centered text looks slightly high.
	public int armorStatusTextVerticalOffset = 0;
	// false shows just "current" instead of "current/max" for damageable items.
	public boolean armorStatusShowMaxDurability = true;
	public ArmorStatusColorMode armorStatusColorMode = ArmorStatusColorMode.FIXED;
	// Also the color of stack counts in GRADIENT mode.
	public int armorStatusColor = 0xFFFFFFFF;
	public ArmorStatusLayoutMode armorStatusLayoutMode = ArmorStatusLayoutMode.BUNDLED;
	public ArmorStatusDirection armorStatusBundledDirection = ArmorStatusDirection.VERTICAL;
	// true: the bundled block sits at the bottom/right of its box and grows up/left - see ArmorStatusBundledHud.
	public boolean armorStatusBundledReversed = false;
	public HudLayout armorStatusBundledHudLayout = new HudLayout();
	// Keyed by ArmorStatusSlot#name(), one layout per slot for INDIVIDUAL mode.
	public Map<String, HudLayout> armorStatusSlotHudLayout = new HashMap<String, HudLayout>();

	/** The layout of one armor status slot, created on first use. */
	public HudLayout armorStatusLayoutFor(ArmorStatusSlot slot) {
		HudLayout layout = this.armorStatusSlotHudLayout.get(slot.name());
		if (layout == null) {
			layout = new HudLayout();
			this.armorStatusSlotHudLayout.put(slot.name(), layout);
		}
		return layout;
	}

	// Custom crosshair: a shape from the preset library or one drawn on a grid, in a color of its
	// own (white by default, so switching it on changes nothing until a color is picked). The size
	// is the edge of one grid cell - in GUI-scale units, or in screen pixels with
	// crosshairIgnoreGuiScale. While aiming at a mob a left click would hit, it can change to a
	// second color and/or a second shape, each switched on separately.
	public boolean customCrosshairEnabled = false;
	public int customCrosshairColor = 0xFFFFFFFF;
	public CrosshairMode crosshairMode = CrosshairMode.PRESET;
	public CrosshairPreset crosshairPreset = CrosshairPreset.SMALLER;
	public boolean[][] crosshairCustomGrid = CrosshairGrid.empty();
	public int crosshairPixelSize = 2;
	public boolean crosshairIgnoreGuiScale = false;
	public boolean crosshairTargetColorEnabled = false;
	public int crosshairTargetColor = 0xFFFF5555;
	// The default differs from the crosshair's own, so switching it on has an obvious effect.
	public boolean crosshairTargetShapeEnabled = false;
	public CrosshairMode crosshairTargetShapeMode = CrosshairMode.PRESET;
	public CrosshairPreset crosshairTargetShapePreset = CrosshairPreset.CIRCLE_DOT;
	public boolean[][] crosshairTargetShapeCustomGrid = CrosshairGrid.empty();

	// Fullbright: the world is drawn at full brightness regardless of the actual light level.
	public boolean fullbrightEnabled = false;

	// Hold-to-zoom. The zoom level is the field of view while zooming - changed with the mouse wheel
	// and remembered. The mouse is slowed down to this share of its normal speed while zooming.
	public boolean zoomEnabled = false;
	public int zoomFov = 15;
	public int zoomSensitivityPercent = 40;

	public static ClientConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	/** `config/` inside the instance's game folder - where the Fabric versions keep theirs too. */
	private static Path file() {
		return new File(MinecraftClient.getInstance().runDirectory, "config").toPath().resolve("tntsallin1client.json");
	}

	private static ClientConfig load() {
		Path file = file();
		if (Files.exists(file)) {
			try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | JsonParseException e) {
				// A config that can't be read must never keep the game from starting.
				LOGGER.warn("Failed to read config, falling back to defaults.", e);
			}
		}

		ClientConfig defaults = new ClientConfig();
		defaults.save();
		return defaults;
	}

	public void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to save config.", e);
		}
	}
}
