package com.tntsallin1client.hud;

/** Whether the Armor & Tool Status display is one HUD element or one per slot. */
public enum ArmorStatusLayoutMode {
	/** Every slot is its own element, movable on its own ({@link ArmorStatusSlotHud}). */
	INDIVIDUAL,
	/** All slots form one block ({@link ArmorStatusBundledHud}). */
	BUNDLED
}
