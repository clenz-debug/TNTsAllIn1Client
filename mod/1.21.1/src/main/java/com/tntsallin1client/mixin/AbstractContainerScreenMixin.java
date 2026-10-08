package com.tntsallin1client.mixin;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Phase 5k rebuild, part 1/2: captures the hovered slot's item (if any) for
 * {@link ShulkerPreviewRenderer} once {@code hoveredSlot} is up to date for
 * this frame. See {@link ShulkerPreviewRenderer} for why this is split
 * across two mixins instead of one.
 *
 * <p>{@code AbstractContainerScreen#render} is where {@code hoveredSlot} gets
 * refreshed, and in 1.21.1 every container screen still goes through it - the
 * recipe book screens (player inventory, crafting table, furnaces) call
 * {@code super.render(...)} from their own {@code render} (the newer versions
 * split that off into {@code renderContents}).
 *
 * <p>Phase 5r ("dark inventory") briefly added a rendering-based darkening
 * overlay plus a light-text override here - removed again after live testing
 * kept surfacing new rendering-order/shape/contrast edge cases (creative tab
 * sprite shapes, icon legibility, per-screen label overrides, ...). Replaced
 * with bundling an actual dark-themed resource pack (first the third-party
 * "Default Dark Mode", now our own "TNT Dark Mode" from
 * {@code resourcepacks/dark-mode/}) - the same "let existing assets handle it
 * instead of reinventing rendering" call already made for 5f/5p, and a much
 * better fit here since the problem was fundamentally about texture/color,
 * not logic.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	@Shadow
	private @Nullable Slot hoveredSlot;

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ShulkerPreviewRenderer.capture(
				this.hoveredSlot != null && this.hoveredSlot.hasItem() ? this.hoveredSlot.getItem() : null,
				mouseX, mouseY);
	}

	/**
	 * While the preview grid is shown, the vanilla tooltip is skipped entirely - it would
	 * otherwise sit right behind the grid, and the grid's header already shows the box's name.
	 * Every container screen (recipe book screens and the creative inventory included) goes
	 * through this method for the hovered-slot tooltip.
	 */
	@Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && ShulkerPreviewRenderer.isPreviewable(this.hoveredSlot.getItem())) {
			ci.cancel();
		}
	}
}
