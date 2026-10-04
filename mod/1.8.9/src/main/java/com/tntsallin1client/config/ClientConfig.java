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
import com.tntsallin1client.waypoint.Waypoint;
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

	// Freecam: a key detaches the camera, the player stays frozen and can't act on the world - see
	// FreecamHandler. Speed in blocks per second; the sensitivity is a share of the normal mouse
	// sensitivity (100 = the same).
	public boolean freecamEnabled = false;
	public int freecamSpeed = 10;
	public int freecamSensitivityPercent = 100;

	// Fullbright: the world is drawn at full brightness regardless of the actual light level.
	public boolean fullbrightEnabled = false;

	// F3 Quick Info: an extra block on the debug screen (see QuickInfo).
	public boolean f3QuickInfoEnabled = false;
	// The system info page (see SystemInfoHud). No "enabled" - whether it shows is the F3 key
	// combination's doing and not saved; where it sits and its color are.
	public int systemInfoTextColor = 0xFFFFFFFF;
	public HudLayout systemInfoHudLayout = new HudLayout();

	// A chat message with "Open" and "Copy" links for every new screenshot (see ScreenshotWatcher).
	public boolean screenshotToastEnabled = false;

	// Discord activity: shows in the player's Discord status that they play through this client
	// (see DiscordPresenceManager). Off until switched on; the server's name is the one detail
	// that stays off even then.
	public boolean discordPresenceEnabled = false;
	public boolean discordPresenceShowGameMode = true;
	public boolean discordPresenceShowVersion = true;
	public boolean discordPresenceShowWorldName = true;
	public boolean discordPresenceShowServerName = false;
	public boolean discordPresenceShowElapsedTime = true;

	// Item physics: dropped items don't spin or bob and lie flat on the ground (see ItemTilt).
	public boolean itemTiltEnabled = false;

	// 3D block models: whether the inventory and the hand show their items in 3D too (see Items3d).
	// The row's own switch isn't here - it is whether the pack is among the game's active ones.
	public boolean blockModels3dItems = true;

	// Own 3D skin layers (see SkinLayers3d). On unless switched off, as in the Fabric versions.
	// Depth is how far the layer stands off the body, in percent of a model pixel; distance is in
	// blocks, beyond it players keep the flat layers.
	public boolean skinLayers3dEnabled = true;
	public boolean skinLayers3dHead = true;
	public boolean skinLayers3dJacket = true;
	public boolean skinLayers3dSleeves = true;
	public boolean skinLayers3dPants = true;
	public int skinLayers3dDepthPercent = 30;
	public int skinLayers3dDistance = 16;

	// No fog: removes the chosen kinds of fog (see NoFog). The fog at the edge of the view distance
	// (incl. the Nether's and the one inside clouds) is the one people usually mean, so it's the
	// only one on by default. Blindness is never touched.
	public boolean noFogEnabled = false;
	public boolean noFogDistance = true;
	public boolean noFogWater = false;
	public boolean noFogLava = false;

	// Quick sort: a "Sort" button and key on the inventory screen (see InventorySorter).
	public boolean quickSortEnabled = false;
	public boolean quickSortGroupByCategory = false;

	// How many inventory click packets leave per game tick (see ContainerClickPacing) - a burst of
	// them, as the quick sort makes, can trip a server's packet limit. 1-20, clamped where it is used.
	public int containerClickPacketsPerTick = 3;

	// Shows the capes players set in the launcher (see ClientCapes). On unless switched off - the
	// one exception to "off until switched on": a cape is set on purpose, and seen by others.
	public boolean clientCapesEnabled = true;

	// Hitbox color: the box F3+B draws around entities in a color of the player's choice (white by
	// default, the game's own, so switching it on changes nothing until a color is picked). The
	// marks inside the box - eye height, view direction - are off until switched on then, each with
	// a color that defaults to the game's own for it.
	public boolean customHitboxColorEnabled = false;
	public int customHitboxColor = 0xFFFFFFFF;
	public boolean customHitboxShowEyeHeight = false;
	public int customHitboxEyeHeightColor = 0xFFFF0000;
	public boolean customHitboxShowViewDirection = false;
	public int customHitboxViewDirectionColor = 0xFF0000FF;

	// Block outline color: the outline around the block being looked at. Black by default, the
	// game's own. Only the color is replaced - the outline stays see-through.
	public boolean customBlockOutlineColorEnabled = false;
	public int customBlockOutlineColor = 0xFF000000;

	// Hold-to-zoom. The zoom level is the field of view while zooming - changed with the mouse wheel
	// and remembered. The mouse is slowed down to this share of its normal speed while zooming.
	public boolean zoomEnabled = false;
	public int zoomFov = 15;
	public int zoomSensitivityPercent = 40;

	// Light level overlay: a key marks the places nearby where hostile mobs can spawn with colored X
	// marks on the ground (see SpawnOverlayRenderer). The key is either held or switches the overlay on and off.
	public boolean spawnOverlayEnabled = false;
	public boolean spawnOverlayHoldMode = true;

	// Waypoints: markers the player sets, drawn into the world as a beam, a block outline and a name
	// (see WaypointRenderer), with their own list and edit screens - reachable from the mod menu and
	// by a key of their own.
	public boolean waypointsEnabled = false;
	// One list per world or server (the key is WaypointScope#currentKey) - see waypointsFor.
	public Map<String, List<Waypoint>> waypointsByWorld = new HashMap<String, List<Waypoint>>();
	// What a new waypoint starts with - each waypoint has its own copy of these four.
	public boolean waypointShowBeam = true;
	public boolean waypointShowMarker = true;
	public boolean waypointShowDistance = true;
	public boolean waypointFadeNearby = false;
	// Whether deleting one waypoint asks first - "Delete All" always does.
	public boolean waypointConfirmDelete = true;
	// An arrowhead at the screen edge towards every waypoint outside the field of view (see WaypointArrowHud).
	public boolean waypointOffscreenArrows = true;

	/** The waypoints of one world or server, created on first use - callers add to and remove from the list itself. */
	public List<Waypoint> waypointsFor(String worldKey) {
		List<Waypoint> waypoints = this.waypointsByWorld.get(worldKey);
		if (waypoints == null) {
			waypoints = new ArrayList<Waypoint>();
			this.waypointsByWorld.put(worldKey, waypoints);
		}
		return waypoints;
	}

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
