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

	// 5d: hold F3 and press S to toggle the CPU/GPU/version overlay. Defaults to
	// "S" (not unbound) to match vanilla's own F3+<letter> muscle memory - this
	// physical key is free in this MC version's Options.java defaults, and even
	// if it weren't, multiple KeyMappings can share a key without conflict (same
	// as vanilla's F3+A/F3+B etc. sharing keys with movement/inventory binds).
	public static final KeyMapping SYSTEM_INFO = new KeyMapping(
			"key.tntsallin1client.system_info",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_S,
			CATEGORY
	);

	private ModKeyBindings() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(SORT_INVENTORY);
		KeyBindingHelper.registerKeyBinding(OPEN_MENU);
		KeyBindingHelper.registerKeyBinding(SYSTEM_INFO);
	}
}
