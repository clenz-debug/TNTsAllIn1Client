package com.tntsallin1client.hud;

import java.util.Locale;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * The equipment slots the Armor & Tool Status display covers - the four armor pieces top to bottom,
 * then the held item. Same role as {@link KeystrokeKey}: a stable identifier the config's per-slot
 * maps key off of. Same names as in the Fabric versions of the mod, minus the off hand this version
 * of the game doesn't have.
 */
public enum ArmorStatusSlot {
	HEAD,
	CHEST,
	LEGS,
	FEET,
	MAINHAND;

	/** The game keeps the worn armor feet first. */
	private static final int HELMET_INDEX = 3;

	/** Translation key of the slot's name. */
	public String labelKey() {
		return "gui.tntsallin1client.armor_status_slot." + name().toLowerCase(Locale.ROOT);
	}

	/** What the player wears or holds there, or null. */
	public ItemStack stack(PlayerEntity player) {
		if (this == MAINHAND) {
			return player.inventory.getMainHandStack();
		}
		return player.inventory.armor[HELMET_INDEX - ordinal()];
	}
}
