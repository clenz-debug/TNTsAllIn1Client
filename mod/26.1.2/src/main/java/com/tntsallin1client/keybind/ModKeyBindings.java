package com.tntsallin1client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "main"));

	// Unbound by default - the user opts in via the vanilla Controls screen,
	// same place the ingame menu (5e) also surfaces it (both edit the same
	// KeyMapping instance, so they can never fall out of sync with each other).
	public static final KeyMapping SORT_INVENTORY = new KeyMapping(
			"key.tntsallin1client.sort_inventory",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// 5e: opens the mod menu directly from gameplay, in addition to the pause menu button.
	public static final KeyMapping OPEN_MENU = new KeyMapping(
			"key.tntsallin1client.open_menu",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// 5d: hold F3 and press this key to toggle the CPU/GPU/version overlay.
	// Defaults to "K" - S was the obvious first pick but is already vanilla's
	// "Dump Dynamic Textures" (key.debug.dumpDynamicTextures, Options.java line
	// ~644 - easy to miss since it's a multi-line KeyMapping() call). Verified
	// K against every keyDebugXxx default in Options.java this time, not just
	// grepped. Rebindable both via vanilla Controls and this mod's own
	// F3OptionsScreen (same underlying KeyMapping either way).
	public static final KeyMapping SYSTEM_INFO = new KeyMapping(
			"key.tntsallin1client.system_info",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			CATEGORY
	);

	// 5h: hold to zoom in. Unbound by default like most binds here (not SYSTEM_INFO's
	// F3-modifier special case) - the user picks a key that doesn't collide with
	// anything they already use.
	public static final KeyMapping ZOOM = new KeyMapping(
			"key.tntsallin1client.zoom",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// 5k: hold while hovering a shulker box to see its remaining contents beyond
	// the 5 items vanilla's own tooltip already lists.
	public static final KeyMapping SHULKER_PREVIEW = new KeyMapping(
			"key.tntsallin1client.shulker_preview",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// 5j redesigned: shows/hides the mob-spawn overlay - hold or toggle,
	// selectable in the mod menu (see ClientConfig#spawnOverlayHoldMode).
	public static final KeyMapping SPAWN_OVERLAY = new KeyMapping(
			"key.tntsallin1client.spawn_overlay",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// 5ah: press while hovering a recipe in the crafting-table/inventory recipe book
	// to pin/unpin it. Unbound by default like most binds here.
	public static final KeyMapping PIN_RECIPE = new KeyMapping(
			"key.tntsallin1client.pin_recipe",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// Waypoint system: opens the waypoint list/management screen directly from gameplay,
	// same "own keybind in addition to the mod menu" pattern as OPEN_MENU itself.
	// Unbound by default like most binds here.
	public static final KeyMapping OPEN_WAYPOINTS = new KeyMapping(
			"key.tntsallin1client.open_waypoints",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// Waypoint system follow-up: opens WaypointCreateScreen directly from gameplay - same screen
	// (with its name field) as OPEN_WAYPOINTS -> "New Waypoint", just skipping the list screen in
	// between. Unbound by default like most binds here.
	public static final KeyMapping CREATE_WAYPOINT = new KeyMapping(
			"key.tntsallin1client.create_waypoint",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	// Freecam: press to enter, press again (or Esc) to exit - see FreecamHandler. Unbound by
	// default like most binds here.
	public static final KeyMapping TOGGLE_FREECAM = new KeyMapping(
			"key.tntsallin1client.toggle_freecam",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	private ModKeyBindings() {
	}

	public static void register() {
		KeyMappingHelper.registerKeyMapping(SORT_INVENTORY);
		KeyMappingHelper.registerKeyMapping(OPEN_MENU);
		KeyMappingHelper.registerKeyMapping(SYSTEM_INFO);
		KeyMappingHelper.registerKeyMapping(ZOOM);
		KeyMappingHelper.registerKeyMapping(SHULKER_PREVIEW);
		KeyMappingHelper.registerKeyMapping(SPAWN_OVERLAY);
		KeyMappingHelper.registerKeyMapping(PIN_RECIPE);
		KeyMappingHelper.registerKeyMapping(OPEN_WAYPOINTS);
		KeyMappingHelper.registerKeyMapping(CREATE_WAYPOINT);
		KeyMappingHelper.registerKeyMapping(TOGGLE_FREECAM);
	}
}
