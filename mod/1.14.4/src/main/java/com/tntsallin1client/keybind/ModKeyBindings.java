package com.tntsallin1client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public final class ModKeyBindings {
	private static final String CATEGORY = "key.category.tntsallin1client.main";
	/** Shows as "Not bound" in the Controls screen. */
	private static final int UNBOUND = InputConstants.UNKNOWN.getValue();

	// Opens the mod menu directly from gameplay, in addition to the pause menu button. Unbound by
	// default - the user picks a key in the vanilla Controls screen.
	public static final KeyMapping OPEN_MENU = new KeyMapping("key.tntsallin1client.open_menu", UNBOUND, CATEGORY);

	// Hold to zoom in. Unbound by default as well - also settable in the zoom's own options screen.
	public static final KeyMapping ZOOM = new KeyMapping("key.tntsallin1client.zoom", UNBOUND, CATEGORY);

	// Switches freecam on and off. Unbound by default - also settable in the freecam options screen.
	public static final KeyMapping TOGGLE_FREECAM = new KeyMapping("key.tntsallin1client.toggle_freecam", UNBOUND, CATEGORY);

	// Opens the waypoint list directly from gameplay. Unbound by default - also settable in the waypoint options screen.
	public static final KeyMapping OPEN_WAYPOINTS = new KeyMapping("key.tntsallin1client.open_waypoints", UNBOUND, CATEGORY);

	// Opens the New Waypoint screen directly from gameplay, skipping the list. Unbound by default as well.
	public static final KeyMapping CREATE_WAYPOINT = new KeyMapping("key.tntsallin1client.create_waypoint", UNBOUND, CATEGORY);

	// Shows the light level overlay - held or as an on/off switch, see ClientConfig#spawnOverlayHoldMode. Unbound by default as well.
	public static final KeyMapping SPAWN_OVERLAY = new KeyMapping("key.tntsallin1client.spawn_overlay", UNBOUND, CATEGORY);

	// Sorts the inventory while the inventory screen is open, like its Sort button. Unbound by default as well.
	public static final KeyMapping SORT_INVENTORY = new KeyMapping("key.tntsallin1client.sort_inventory", UNBOUND, CATEGORY);

	// Held over a shulker box in an inventory: shows what is in it. Unbound by default as well - also settable in the preview options screen.
	public static final KeyMapping SHULKER_PREVIEW = new KeyMapping("key.tntsallin1client.shulker_preview", UNBOUND, CATEGORY);

	// Pressed over a recipe in the recipe book: pins it to the HUD, or unpins it. Unbound by default as well.
	public static final KeyMapping PIN_RECIPE = new KeyMapping("key.tntsallin1client.pin_recipe", UNBOUND, CATEGORY);

	// Opens the list of pinned recipes directly from gameplay. Unbound by default as well.
	public static final KeyMapping OPEN_PINNED_RECIPES = new KeyMapping("key.tntsallin1client.open_pinned_recipes", UNBOUND, CATEGORY);

	private ModKeyBindings() {
	}

	/** Puts our key bindings into the game's Controls screen and `options.txt`. */
	public static void register() {
		KeyBindingHelper.registerKeyBinding(OPEN_MENU);
		KeyBindingHelper.registerKeyBinding(ZOOM);
		KeyBindingHelper.registerKeyBinding(TOGGLE_FREECAM);
		KeyBindingHelper.registerKeyBinding(OPEN_WAYPOINTS);
		KeyBindingHelper.registerKeyBinding(CREATE_WAYPOINT);
		KeyBindingHelper.registerKeyBinding(SPAWN_OVERLAY);
		KeyBindingHelper.registerKeyBinding(SORT_INVENTORY);
		KeyBindingHelper.registerKeyBinding(SHULKER_PREVIEW);
		KeyBindingHelper.registerKeyBinding(PIN_RECIPE);
		KeyBindingHelper.registerKeyBinding(OPEN_PINNED_RECIPES);
	}
}
