package com.tntsallin1client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.crosshair.CrosshairGrid;
import com.tntsallin1client.crosshair.CrosshairMode;
import com.tntsallin1client.crosshair.CrosshairPreset;
import com.tntsallin1client.hud.ArmorStatusColorMode;
import com.tntsallin1client.hud.ArmorStatusDirection;
import com.tntsallin1client.hud.ArmorStatusIconPosition;
import com.tntsallin1client.hud.ArmorStatusLayoutMode;
import com.tntsallin1client.hud.ArmorStatusSlot;
import com.tntsallin1client.hud.HudLayout;
import com.tntsallin1client.recipe.PinnedRecipe;
import com.tntsallin1client.waypoint.Waypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	public boolean coordinatesHudEnabled = false;
	public boolean coordinatesHudShowCoordinates = true;
	public boolean coordinatesHudShowDirection = true;
	public boolean coordinatesHudShowDegrees = true;
	public HudLayout coordinatesHudLayout = new HudLayout();

	// 5b, renamed 5ag ("Material Counter" -> "Item Counter"): which item to tally
	// across the inventory. Either a fixed item id, or whatever is currently in
	// the main hand.
	public boolean itemCounterEnabled = false;
	public boolean itemCounterUseHeldItem = false;
	public String itemCounterItemId = "minecraft:diamond";
	public HudLayout itemCounterHudLayout = new HudLayout();
	public boolean itemCounterShowItemIcon = false;

	// 5c: quick-sort button + keybind in the player's own inventory screen.
	public boolean quickSortEnabled = false;
	public boolean quickSortGroupByCategory = false;

	// Defensive: paces outgoing container-click packets (see ContainerClickPacingHandler) so a
	// burst - fast manual clicking, drag-across-slots, this mod's own InventorySorter - can't
	// trip a server's packet-rate/anti-dupe limiter ("Too many suspicious packets" kicks).
	// Defaults to enabled, unlike this file's other feature flags - it's a pure safety net with
	// no noticeable downside, meant to protect the player automatically on any server without
	// them needing to know to turn it on.
	public boolean containerClickPacingEnabled = true;
	// Packets allowed to leave per client tick before extras start queuing. Not exposed as a UI
	// control in v1 - hand-edit this file if a lower/higher pace is ever needed. Valid range
	// 1-20 (clamped defensively at the point of use, see ContainerClickPacingHandler#tick).
	public int containerClickPacketsPerTick = 3;

	// 5d: extra "Quick Info" block on the F3 debug screen.
	public boolean f3QuickInfoEnabled = false;

	// Phase 2 leftover: the "TNT's All-In-1 Client (Mixin active)" top-left label.
	public boolean clientNameLabelEnabled = false;

	// 5g: always-visible FPS counter, no F3 needed.
	public boolean fpsCounterEnabled = false;
	public HudLayout fpsCounterHudLayout = new HudLayout();

	// Own wishlist item: always-visible latency (ping) counter, same shape as the FPS counter above.
	public boolean latencyHudEnabled = false;
	public HudLayout latencyHudLayout = new HudLayout();

	// 5h: hold-to-zoom.
	public boolean zoomEnabled = false;
	// Scroll-adjustable while zooming (see ZoomHandler); persists as the
	// "remembered" zoom level between sessions, same as every other setting here.
	public int zoomFov = 15;
	// Vanilla's own mouse-turn speed isn't scaled by FOV, so at a narrow zoom FOV
	// the same physical mouse movement swings the view across a much larger share
	// of the (now much smaller) visible angle - feels like sensitivity spiked.
	// This scales the player's normal sensitivity down to this percentage while
	// zoomed (see ZoomHandler), then restores it on release.
	public int zoomSensitivityPercent = 40;

	// Freecam: keybind-toggled detached camera, real player frozen in place (no movement, no
	// packets sent) while active - collision-checked against real world geometry (doors/fence
	// gates/trapdoors always passable, open or closed) so it can't be used to see through walls,
	// and all attack/mine/place/use interaction is blocked while active. See FreecamHandler.
	public boolean freecamEnabled = false;
	// Blocks/sec at normal (non-sprint) speed.
	public int freecamSpeed = 10;
	// 100 = matches the player's normal mouse-look sensitivity exactly; lower/higher lets freecam
	// feel different from that without touching the normal sensitivity setting itself.
	public int freecamSensitivityPercent = 100;

	// 5i redesigned: custom crosshair color (defaults to white so turning this
	// on doesn't visibly change anything until a color is picked), a shape
	// (preset library or user-drawn 9x9 grid), a size independent of GUI Scale,
	// and an optional second color while aiming at an attackable mob.
	public boolean customCrosshairEnabled = false;
	public int customCrosshairColor = 0xFFFFFFFF;
	public CrosshairMode crosshairMode = CrosshairMode.PRESET;
	public CrosshairPreset crosshairPreset = CrosshairPreset.SMALLER;
	public boolean[][] crosshairCustomGrid = CrosshairGrid.empty();
	public int crosshairPixelSize = 2;
	public boolean crosshairIgnoreGuiScale = false;
	public boolean crosshairTargetColorEnabled = false;
	public int crosshairTargetColor = 0xFFFF5555;

	// 5ac: optional second *shape* (same preset-or-custom-grid representation
	// as the base crosshair) while aiming at an attackable mob, independent of
	// the target-color toggle above. Default preset deliberately different from
	// the base crosshair's own SMALLER default, so turning this on has an
	// obvious effect immediately.
	public boolean crosshairTargetShapeEnabled = false;
	public CrosshairMode crosshairTargetShapeMode = CrosshairMode.PRESET;
	public CrosshairPreset crosshairTargetShapePreset = CrosshairPreset.CIRCLE_DOT;
	public boolean[][] crosshairTargetShapeCustomGrid = CrosshairGrid.empty();

	// 5j: fullbright (forced gamma override).
	public boolean fullbrightEnabled = false;

	// 5j redesigned: was a plain "Light: N" text HUD line; replaced by a
	// key-triggered in-world overlay marking nearby mob-spawnable positions
	// with colored X marks (see SpawnOverlayRenderer). Hold-vs-toggle is a
	// separate setting since either can be the more comfortable one depending
	// on how someone plays.
	public boolean spawnOverlayEnabled = false;
	public boolean spawnOverlayHoldMode = true;

	// 5k: hold-to-preview shulker box contents beyond vanilla's own 5-item cap.
	public boolean shulkerPreviewEnabled = false;

	// 5l: F3+B hitbox outline color. Defaults to white, vanilla's own color, so
	// turning this on doesn't visibly change anything until a color is picked.
	public boolean customHitboxColorEnabled = false;
	public int customHitboxColor = 0xFFFFFFFF;

	// 5aa/5ab: the four secondary F3+B indicators vanilla draws alongside the
	// main hitbox - each independently toggleable and colorable so a chosen
	// main hitbox color can't end up matching (and visually blending into) one
	// of them. Colors default to vanilla's own hardcoded ones for each.
	public boolean customHitboxShowEyeHeight = false;
	public int customHitboxEyeHeightColor = 0xFFFF0000;
	public boolean customHitboxShowVehicleMarker = false;
	public int customHitboxVehicleMarkerColor = 0xFFFFFF00;
	public boolean customHitboxShowViewDirection = false;
	public int customHitboxViewDirectionColor = 0xFF0000FF;
	public boolean customHitboxShowDragonParts = false;
	public int customHitboxDragonPartsColor = 0xFF3FFF00;

	// 5q: the black wireframe box drawn around whatever block is being looked
	// at. Same "alpha byte never shown as-is" pattern as keystrokesActiveColor -
	// vanilla's own alpha (translucent normally, opaque in high-contrast mode)
	// is always kept, only the RGB comes from here. Defaults to black, vanilla's
	// own color, so turning this on doesn't visibly change anything until picked.
	public boolean customBlockOutlineColorEnabled = false;
	public int customBlockOutlineColor = 0xFF000000;

	// 5m: keystrokes overlay (WASD/Shift/Space/mouse buttons + sprint/drop).
	// Active-box color, same green as the original hardcoded default; the
	// alpha byte here is never actually shown as-is (KeystrokesHud always
	// re-applies its own translucent glow alpha on top), only the RGB matters.
	public boolean keystrokesEnabled = false;
	public HudLayout keystrokesHudLayout = new HudLayout();
	public int keystrokesActiveColor = 0xFF33CC33;
	// Per-key on/off, keyed by KeystrokeKey#name() - a key absent from the map
	// (the common case, nothing has been toggled off yet) counts as enabled.
	public Map<String, Boolean> keystrokesKeyEnabled = new HashMap<>();

	// 5n: Open/Copy popup after taking a screenshot.
	public boolean screenshotToastEnabled = false;

	// 5o: cosmetic dropped-item lean/tilt ("item physics" light).
	public boolean itemTiltEnabled = false;

	// 5t: text colors for the plain-text HUD/overlay elements, all defaulting to
	// white (vanilla's own text color) so adding this doesn't visibly change
	// anything until picked. Edited from each feature's own options screen,
	// same as every other color setting in this file.
	public int coordinatesHudTextColor = 0xFFFFFFFF;
	public int itemCounterTextColor = 0xFFFFFFFF;
	public int fpsCounterTextColor = 0xFFFFFFFF;
	public int latencyTextColor = 0xFFFFFFFF;
	public int clientNameLabelColor = 0xFFFFFFFF;
	public int systemInfoTextColor = 0xFFFFFFFF;
	// No "enabled" flag - visibility is the transient F3+S toggle (SystemInfoOverlay#visible), not
	// persisted, same as vanilla's own F3 screen. Position/scale still persist like every other HUD
	// element though, so it doesn't matter that "customPosition" starts false on a fresh install.
	public HudLayout systemInfoHudLayout = new HudLayout();
	public int keystrokesTextColor = 0xFFFFFFFF;

	// 5ah: recipes pinned from the crafting-table/inventory recipe book, shown as a movable HUD
	// reminder - up to PinnedRecipeManager#MAX_PINNED at once (own user request, "maximal
	// Hauptrezepte 5 Rezepte"), each independently show/hide-able (PinnedRecipe#visible) and
	// removable via PinnedRecipeListScreen. Empty until the player actually pins something.
	public boolean pinnedRecipeEnabled = false;
	public List<PinnedRecipe> pinnedRecipes = new ArrayList<>();
	public HudLayout pinnedRecipeHudLayout = new HudLayout();
	// Own user request - only the "->" arrow's color.
	public int pinnedRecipeArrowColor = 0xFFFFFFFF;
	// Own follow-up request ("die Farben bei den Zahlen der Hauptrezepte sollen ... einstellbar
	// sein") - the main ingredient/result stack-count badges used to be vanilla's own
	// GuiGraphicsExtractor#itemDecorations rendering, which has no color parameter; PinnedRecipeHud
	// now reproduces that method's own decompiled layout math by hand (renderItemBar/
	// renderItemCooldown/renderItemCount) with the count text's color pulled out as this setting
	// instead of vanilla's hardcoded white (see PinnedRecipeHud#renderStack).
	public int pinnedRecipeCountColor = 0xFFFFFFFF;
	// Own user request - shows each craftable ingredient's own one-level sub-recipe in miniature
	// underneath it (see PinnedIngredient#subIngredients / PinnedRecipeHud).
	public boolean pinnedRecipeShowSubIngredients = false;
	// Own follow-up request ("die Zahlen sollen wie die Pfeile auch von der Farbe her angepasst
	// werden") - the small sub-ingredient counts next to each mini icon are a separate plain text
	// draw from the main counts above, with their own color (see PinnedRecipeHud#drawSubIcons).
	public int pinnedRecipeSubIngredientCountColor = 0xFFFFFFFF;
	// Own follow-up request ("die Items ... nicht verschwinden [lassen] wenn man sie im Inv hat"):
	// off by default (existing behavior) counts down against inventory contents and drops a fully-
	// satisfied ingredient's row entirely; on, every ingredient always shows its full static
	// requirement regardless of what's already in the player's inventory (see
	// PinnedRecipeHud#buildVisibleRows).
	public boolean pinnedRecipeShowFullAmounts = false;

	// Waypoint system: user-created markers shown in-world as a beam + name label (see
	// WaypointRenderer), managed from their own list/edit screens (see WaypointListScreen),
	// reachable both from the mod menu and directly from gameplay via their own keybind.
	public boolean waypointsEnabled = false;
	// Keyed by WaypointScope#currentKey (per singleplayer save / per multiplayer server address) -
	// waypoints from one world never show up in another. See #waypointsFor below.
	public Map<String, List<Waypoint>> waypointsByWorld = new HashMap<>();
	public boolean waypointShowBeam = true;
	public boolean waypointShowMarker = true;
	public boolean waypointShowDistance = true;
	// When enabled, a waypoint's beam/marker/label fade out smoothly as the player gets close,
	// fully invisible within WaypointRenderer's fade-end distance - off by default so turning the
	// feature on doesn't visibly change anything until picked.
	public boolean waypointFadeNearby = false;
	// Whether deleting a single waypoint asks for confirmation first - the "delete all" button
	// always confirms regardless of this setting (see WaypointListScreen).
	public boolean waypointConfirmDelete = true;

	/** The live, mutable waypoint list for one world/server key - callers add/remove/save directly on it. */
	public List<Waypoint> waypointsFor(String worldKey) {
		return this.waypointsByWorld.computeIfAbsent(worldKey, key -> new ArrayList<>());
	}

	// Armor & Tool Status: durability of the four worn armor pieces plus main-hand/offhand, or -
	// for stackable items like blocks - the count in that one stack, never the whole inventory
	// (unlike the item counter above). Each of the six slots is independently toggleable (see
	// armorStatusSlotEnabled, same "absent = enabled" convention as keystrokesKeyEnabled) and can be
	// shown either as six separate, individually movable HUD elements or bundled into a single one
	// (see armorStatusLayoutMode/armorStatusBundledDirection) - see ArmorStatusHud for the render logic.
	public boolean armorStatusEnabled = false;
	public Map<String, Boolean> armorStatusSlotEnabled = new HashMap<>();
	// Display order for the bundled layout (and, for consistency, the per-slot one too) - names
	// from ArmorStatusSlot, first to last/top to bottom. Empty (nothing customized yet) or
	// missing a slot (e.g. after an update adds one) falls back to ArmorStatusSlot's own
	// declaration order for whatever isn't listed - see ArmorStatusHud#orderedSlots.
	public List<String> armorStatusSlotOrder = new ArrayList<>();
	public boolean armorStatusShowName = true;
	public boolean armorStatusShowIcon = true;
	public ArmorStatusIconPosition armorStatusIconPosition = ArmorStatusIconPosition.LEFT;
	// Nudges the durability/count text up (negative) or down (positive) relative to the icon, on
	// top of the centering ArmorStatusHud#drawRow already does by default - Minecraft's font
	// reserves a little blank space below digits for descenders they don't have, so the
	// mathematically-centered text still looks slightly high against the icon; this lets a user
	// dial in a pixel-perfect look instead of that always being slightly off.
	public int armorStatusTextVerticalOffset = 0;
	// false replaces "current/max" with just "current" for damageable items (stack counts are
	// unaffected either way, they were never shown as a fraction).
	public boolean armorStatusShowMaxDurability = true;
	public ArmorStatusColorMode armorStatusColorMode = ArmorStatusColorMode.FIXED;
	// Also the color stackable-item counts always use, even in GRADIENT mode - only damageable
	// items get the durability-based gradient. Defaults to white, vanilla's own text color, so
	// turning this on doesn't visibly change anything until a color is picked.
	public int armorStatusColor = 0xFFFFFFFF;
	public ArmorStatusLayoutMode armorStatusLayoutMode = ArmorStatusLayoutMode.BUNDLED;
	public ArmorStatusDirection armorStatusBundledDirection = ArmorStatusDirection.VERTICAL;
	// false (default): armorStatusBundledHudLayout's x/y is the block's top-left, growing
	// down/right as more slots are populated - same as always. true: x/y is instead the edge the
	// block grows AWAY from (bottom for VERTICAL, right for HORIZONTAL) - e.g. put on a helmet
	// with the display docked bottom-right and it renders right at that anchor; add a chestplate
	// and the helmet gets pushed toward the far edge while the chestplate takes its place at the
	// anchor - per user request, so a corner-docked display doesn't grow away from the corner
	// it's docked to. See ArmorStatusHud#drawBundled and HudEditorScreen#armorStatusBundledBounds.
	public boolean armorStatusBundledReversed = false;
	public HudLayout armorStatusBundledHudLayout = new HudLayout();
	// Keyed by ArmorStatusSlot#name(), one independent HudLayout per slot for INDIVIDUAL mode - see
	// armorStatusLayoutFor below.
	public Map<String, HudLayout> armorStatusSlotHudLayout = new HashMap<>();

	/** The live HudLayout for one armor status slot, created on first use - callers may mutate it directly (same pattern as waypointsFor above). */
	public HudLayout armorStatusLayoutFor(ArmorStatusSlot slot) {
		return this.armorStatusSlotHudLayout.computeIfAbsent(slot.name(), key -> new HudLayout());
	}

	// Discord Rich Presence (own user request, see Ideen_für_den_client.md) - shows what this
	// launcher's mod is doing in the player's Discord status, entirely opt-in. Master toggle off by
	// default (never suddenly visible to others after an update); every sub-toggle below defaults on
	// once the master is, same "off by default overall, everything included once turned on"
	// convention as e.g. coordinatesHudShow* above - except discordPresenceShowServerName, which
	// stays off even then (own explicit request: a multiplayer server's name/address is public-facing
	// info about someone else's server, not just about the player, so it needs its own opt-in on top
	// of the master toggle rather than being swept in with everything else).
	public boolean discordPresenceEnabled = false;
	public boolean discordPresenceShowGameMode = true;
	public boolean discordPresenceShowVersion = true;
	public boolean discordPresenceShowWorldName = true;
	public boolean discordPresenceShowServerName = false;
	public boolean discordPresenceShowElapsedTime = true;
	// No UI control (same "hand-edit this file" precedent as containerClickPacketsPerTick above) -
	// a Discord Application's client ID is a one-time, technical piece of setup (create the
	// Application at discord.com/developers/applications, paste its id here), not something to
	// expose a whole settings-screen text field for. "0" is a deliberately invalid placeholder:
	// DiscordPresenceManager's connect attempt fails fast on it (Discord rejects any unknown
	// client_id) and just quietly retries, same as when Discord itself isn't running at all.
	public String discordApplicationClientId = "0";

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
