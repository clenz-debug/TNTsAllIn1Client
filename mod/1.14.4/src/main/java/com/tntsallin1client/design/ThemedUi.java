package com.tntsallin1client.design;

/**
 * Whether what is being drawn belongs to a screen in the client design. The client design is not
 * brought over to this version yet, so nothing does - the mod's menus are in the Minecraft design.
 * The menu classes already ask here, so the design only has to be switched on in this one place.
 */
public final class ThemedUi {
	private ThemedUi() {
	}

	public static boolean active() {
		return false;
	}
}
