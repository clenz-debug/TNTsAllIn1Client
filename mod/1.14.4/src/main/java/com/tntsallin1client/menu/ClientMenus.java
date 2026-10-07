package com.tntsallin1client.menu;

import com.tntsallin1client.design.ClientDesign;
import net.minecraft.client.gui.screens.Screen;

/** The Client Mods menu in whichever look the current design asks for - every entry point (title screen, pause menu, key) opens it through here. */
public final class ClientMenus {
	private ClientMenus() {
	}

	/** `parent` may be null: opened by its key from the game. */
	public static Screen create(Screen parent) {
		return ClientDesign.isClient() ? new ClientModsCardScreen(parent) : new ClientMenuScreen(parent);
	}
}
