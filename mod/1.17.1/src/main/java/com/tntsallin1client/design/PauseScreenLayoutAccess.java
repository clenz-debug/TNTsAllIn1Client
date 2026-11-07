package com.tntsallin1client.design;

import net.minecraft.client.gui.screens.PauseScreen;

/** Implemented on {@code PauseScreen} by {@code PauseScreenMixin} - lets {@code PauseMenuIntegration} and {@link ThemedUi} tell which layout a pause menu currently shows. */
public interface PauseScreenLayoutAccess {
	boolean tntsallin1client$isClientLayout();

	/** Whether this is the full pause menu, not the bare "Game Menu" title that F3+Esc pauses with. */
	boolean tntsallin1client$showsPauseMenu();

	static boolean showsPauseMenu(PauseScreen screen) {
		return ((PauseScreenLayoutAccess) screen).tntsallin1client$showsPauseMenu();
	}
}
