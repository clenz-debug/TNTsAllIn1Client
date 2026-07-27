package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5r follow-up (user report: "in the creative menu the individual
 * category tab buttons aren't darkened, still bright"): {@code
 * AbstractContainerScreenMixin}'s dark overlay only covers {@code
 * leftPos..leftPos+imageWidth} / {@code topPos..topPos+imageHeight} - exactly
 * the rectangle {@code renderBg} paints the panel texture into. The creative
 * inventory's category tab buttons (Building Blocks, Redstone, Search, ...)
 * are deliberately drawn *outside* that rectangle by design (in the
 * decompiled sources, {@code renderTabButton} draws each 26x32 sprite at
 * either {@code topPos - 32} for the top row or {@code topPos + imageHeight}
 * for the bottom row), so they were never covered by that overlay.
 *
 * <p>{@code getTabX}/{@code getTabY} (the exact per-tab position formula) are
 * private on {@code CreativeModeInventoryScreen} - rather than risk a
 * hand-written bytecode target string to {@code @Shadow} a private method
 * (the same class of mistake 5o's writeup already flagged as unverifiable
 * without a real game launch), the small bit of math is re-derived here from
 * {@link CreativeModeTab}'s own public {@code column()}/{@code row()}/{@code
 * isAlignedRight()} instead of calling the private originals - same result,
 * no private-method Shadow needed. This mixin fake-extends {@code
 * AbstractContainerScreen} (same trick as {@code ItemEntityRendererMixin})
 * purely so {@code leftPos}/{@code topPos}/{@code imageWidth}/{@code
 * imageHeight} (declared there, inherited by {@code CreativeModeInventoryScreen})
 * are ordinary inherited fields at compile time, instead of needing a second
 * cross-class Shadow of fields this mixin's own target doesn't directly
 * declare.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
	private static final int DARK_OVERLAY_COLOR = 0xB0000000;
	private static final int TAB_WIDTH = 26;
	private static final int TAB_HEIGHT = 32;

	protected CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void tntsallin1client$onRenderBgTail(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ClientConfig.get().darkInventoryEnabled) {
			return;
		}

		for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
			int x = this.leftPos + tabX(tab);
			int y = this.topPos + tabY(tab);
			guiGraphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, DARK_OVERLAY_COLOR);
		}
	}

	/** Same formula as the private {@code CreativeModeInventoryScreen#getTabX}, re-derived from public {@link CreativeModeTab} accessors. */
	private int tabX(CreativeModeTab tab) {
		int column = tab.column();
		if (tab.isAlignedRight()) {
			return this.imageWidth - 27 * (7 - column) + 1;
		}
		return 27 * column;
	}

	/** Same formula as the private {@code CreativeModeInventoryScreen#getTabY}. */
	private int tabY(CreativeModeTab tab) {
		return tab.row() == CreativeModeTab.Row.TOP ? -32 : this.imageHeight;
	}
}
