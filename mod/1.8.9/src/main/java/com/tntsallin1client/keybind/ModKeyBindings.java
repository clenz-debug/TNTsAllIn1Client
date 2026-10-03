package com.tntsallin1client.keybind;

import java.util.Arrays;

import net.minecraft.client.option.KeyBinding;

public final class ModKeyBindings {
	private static final String CATEGORY = "key.category.tntsallin1client.main";
	/** LWJGL 2's `Keyboard.KEY_NONE` - shows as "NONE" in the Controls screen. */
	private static final int UNBOUND = 0;

	// Opens the mod menu directly from gameplay, in addition to the pause menu button. Unbound by
	// default - the user picks a key in the vanilla Controls screen.
	public static final KeyBinding OPEN_MENU = new KeyBinding("key.tntsallin1client.open_menu", UNBOUND, CATEGORY);

	// Hold to zoom in. Unbound by default as well - also settable in the zoom's own options screen.
	public static final KeyBinding ZOOM = new KeyBinding("key.tntsallin1client.zoom", UNBOUND, CATEGORY);

	// Switches freecam on and off. Unbound by default - also settable in the freecam's own options screen.
	public static final KeyBinding TOGGLE_FREECAM = new KeyBinding("key.tntsallin1client.toggle_freecam", UNBOUND, CATEGORY);

	// Opens the waypoint list directly from gameplay. Unbound by default - also settable in the waypoints' own options screen.
	public static final KeyBinding OPEN_WAYPOINTS = new KeyBinding("key.tntsallin1client.open_waypoints", UNBOUND, CATEGORY);

	// Opens the "New Waypoint" screen directly from gameplay, skipping the list. Unbound by default as well.
	public static final KeyBinding CREATE_WAYPOINT = new KeyBinding("key.tntsallin1client.create_waypoint", UNBOUND, CATEGORY);

	// Shows the light level overlay - held or as an on/off switch, see ClientConfig#spawnOverlayHoldMode. Unbound by default as well.
	public static final KeyBinding SPAWN_OVERLAY = new KeyBinding("key.tntsallin1client.spawn_overlay", UNBOUND, CATEGORY);

	private static final KeyBinding[] ALL = {OPEN_MENU, ZOOM, TOGGLE_FREECAM, OPEN_WAYPOINTS, CREATE_WAYPOINT, SPAWN_OVERLAY};

	private ModKeyBindings() {
	}

	/**
	 * The game's key bindings plus ours. The game keeps them in one array that the Controls screen
	 * lists and `options.txt` is read into and written from - ours have to be in it before the
	 * options are loaded (see `GameOptionsMixin`).
	 */
	public static KeyBinding[] appendTo(KeyBinding[] keys) {
		if (Arrays.asList(keys).contains(OPEN_MENU)) {
			return keys;
		}
		KeyBinding[] result = Arrays.copyOf(keys, keys.length + ALL.length);
		System.arraycopy(ALL, 0, result, keys.length, ALL.length);
		return result;
	}
}
