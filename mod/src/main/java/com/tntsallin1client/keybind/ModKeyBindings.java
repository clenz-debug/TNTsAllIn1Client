package com.tntsallin1client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

	private ModKeyBindings() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(SORT_INVENTORY);
		KeyBindingHelper.registerKeyBinding(OPEN_MENU);
		KeyBindingHelper.registerKeyBinding(SYSTEM_INFO);
		KeyBindingHelper.registerKeyBinding(ZOOM);
		KeyBindingHelper.registerKeyBinding(SHULKER_PREVIEW);
		KeyBindingHelper.registerKeyBinding(SPAWN_OVERLAY);
	}
}
