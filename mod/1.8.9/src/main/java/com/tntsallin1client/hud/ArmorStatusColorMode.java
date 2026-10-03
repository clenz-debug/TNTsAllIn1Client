package com.tntsallin1client.hud;

/** How the Armor & Tool Status display colors its numbers. */
public enum ArmorStatusColorMode {
	/** Always the one color the player picked. */
	FIXED,
	/** Green at full durability fading through yellow to red near breaking; stack counts keep the picked color. */
	GRADIENT
}
