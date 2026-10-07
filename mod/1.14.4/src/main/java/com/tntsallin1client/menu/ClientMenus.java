package com.tntsallin1client.menu;

import net.minecraft.client.gui.screens.Screen;

/**
 * The Client Mods menu - every entry point (title screen, pause menu, key) opens it through here.
 * The card menu of the client design is not brought over to this version yet, so it is always the
 * list menu.
 */
public final class ClientMenus {
	private ClientMenus() {
	}

	/** `parent` may be null: opened by its key from the game. */
	public static Screen create(Screen parent) {
		return new ClientMenuScreen(parent);
	}
}
