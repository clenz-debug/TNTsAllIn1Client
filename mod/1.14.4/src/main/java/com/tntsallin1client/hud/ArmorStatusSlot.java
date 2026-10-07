package com.tntsallin1client.hud;

import java.util.Locale;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The six equipment slots the Armor & Tool Status display covers - the four armor pieces top to
 * bottom, then main hand and off hand. Same role as {@link KeystrokeKey}: a stable identifier the
 * config's per-slot maps key off of. Same names as in the other versions of the mod.
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

	/** Translation key of the slot's name. */
	public String labelKey() {
		return "gui.tntsallin1client.armor_status_slot." + name().toLowerCase(Locale.ROOT);
	}

	/** What the player wears or holds there - an empty stack if nothing. */
	public ItemStack stack(Player player) {
		return player.getItemBySlot(this.vanillaSlot);
	}
}
