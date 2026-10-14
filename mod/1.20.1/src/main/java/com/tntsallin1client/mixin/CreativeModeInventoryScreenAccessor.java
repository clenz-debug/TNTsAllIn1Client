package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The creative inventory's open tab, for the sort button. Fabric API for this version has no public
 * {@code getSelectedItemGroup()} yet; the game keeps the tab in a private static field.
 */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
	@Accessor("selectedTab")
	static CreativeModeTab tntsallin1client$getSelectedTab() {
		throw new AssertionError();
	}
}
