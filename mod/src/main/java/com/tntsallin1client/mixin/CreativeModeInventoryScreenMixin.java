package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
 *
 * <p>Bugfix round 2 (screenshot from live test): the original version darkened
 * each tab with a plain {@code guiGraphics.fill(...)} rectangle. The tab
 * sprite itself isn't a plain rectangle (rounded/notched "folder tab" shape
 * with transparent corners) - a flat rectangular fill on top ignored that
 * transparency and showed up as a visible hard-edged black box poking out
 * past the actual tab outline. Fixed by re-drawing the *same* sprite (same
 * identifier-selection logic vanilla's own {@code renderTabButton} uses, just
 * re-derived here since the originals are private - same reasoning as
 * {@code tabX}/{@code tabY} above) with a translucent tint color instead of a
 * plain rectangle: {@code blitSprite}'s color parameter alpha-blends onto
 * whatever vanilla already drew, so only pixels the sprite itself actually
 * covers get darkened - transparent corners stay transparent, no more box.
 * The item icon is then re-rendered on top at full brightness, since it'd
 * otherwise be darkened along with the tab background it sits on.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
	private static final int DARK_OVERLAY_COLOR = 0xB0000000;
	private static final int TAB_WIDTH = 26;
	private static final int TAB_HEIGHT = 32;

	@Shadow
	@Final
	private static Identifier[] UNSELECTED_TOP_TABS;
	@Shadow
	@Final
	private static Identifier[] SELECTED_TOP_TABS;
	@Shadow
	@Final
	private static Identifier[] UNSELECTED_BOTTOM_TABS;
	@Shadow
	@Final
	private static Identifier[] SELECTED_BOTTOM_TABS;
	@Shadow
	private static CreativeModeTab selectedTab;

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
			boolean top = tab.row() == CreativeModeTab.Row.TOP;
			boolean selected = tab == selectedTab;
			Identifier[] identifiers = top
					? (selected ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS)
					: (selected ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS);
			Identifier sprite = identifiers[Mth.clamp(tab.column(), 0, identifiers.length - 1)];
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, TAB_HEIGHT, DARK_OVERLAY_COLOR);

			int iconX = x + 13 - 8;
			int iconY = y + 16 - 8 + (top ? 1 : -1);
			guiGraphics.renderItem(tab.getIconItem(), iconX, iconY);
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
