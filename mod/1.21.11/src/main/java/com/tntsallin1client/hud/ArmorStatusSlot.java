package com.tntsallin1client.hud;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * The six equipment slots the Armor & Tool Status display covers - the four
 * armor pieces (vanilla's own top-to-bottom order: helmet, chestplate,
 * leggings, boots) followed by main-hand and offhand. Same role as
 * {@link KeystrokeKey}: a stable identifier the config's per-slot
 * enabled/layout maps key off of, independent of anything display-related.
 */
public enum ArmorStatusSlot {
	HEAD(EquipmentSlot.HEAD),
	CHEST(EquipmentSlot.CHEST),
	LEGS(EquipmentSlot.LEGS),
	FEET(EquipmentSlot.FEET),
	MAINHAND(EquipmentSlot.MAINHAND),
	OFFHAND(EquipmentSlot.OFFHAND);

	public final EquipmentSlot vanillaSlot;

	ArmorStatusSlot(EquipmentSlot vanillaSlot) {
		this.vanillaSlot = vanillaSlot;
	}
}
