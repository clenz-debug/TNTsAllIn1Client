package com.tntsallin1client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ModKeyBindings {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "main"));

	// Unbound by default - the user opts in via the vanilla Controls screen,
	// same place the ingame menu (5e) will eventually surface it too.
	public static final KeyMapping SORT_INVENTORY = new KeyMapping(
			"key.tntsallin1client.sort_inventory",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			CATEGORY
	);

	private ModKeyBindings() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(SORT_INVENTORY);
	}
}
