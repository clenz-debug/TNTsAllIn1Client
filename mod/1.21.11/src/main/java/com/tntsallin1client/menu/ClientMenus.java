package com.tntsallin1client.menu;

import com.tntsallin1client.design.ClientDesign;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** The Client Mods menu in whichever look the current design asks for - every entry point (title screen, pause menu, keybind) opens it through here. */
public final class ClientMenus {
	private ClientMenus() {
	}

	public static Screen create(@Nullable Screen parent) {
		return ClientDesign.isClient() ? new ClientModsCardScreen(parent) : new ClientMenuScreen(parent);
	}
}
