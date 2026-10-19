package com.tntsallin1client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The creative inventory's open tab, for the sort button. Fabric API for this version has no public
 * {@code getSelectedItemGroup()} yet; the game keeps the tab in a private static field.
 */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
	/** In 1.19.2 the index into {@code CreativeModeTab.TABS}. */
	@Accessor("selectedTab")
	static int tntsallin1client$getSelectedTab() {
		throw new AssertionError();
	}
}
