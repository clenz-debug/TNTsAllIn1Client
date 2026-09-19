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
import org.jspecify.annotations.Nullable;

/**
 * Phase 5k rebuild, part 1/2: captures the hovered slot's item (if any) for
 * {@link ShulkerPreviewRenderer} once {@code hoveredSlot} is up to date for
 * this frame. See {@link ShulkerPreviewRenderer} for why this is split
 * across two mixins instead of one.
 *
 * <p><b>Bugfix (crafting table / furnace / player inventory never showed a
 * preview):</b> this used to inject into {@code AbstractContainerScreen#render}.
 * That method is only reliably called for screens that don't override
 * {@code render()} themselves (chests, villager trading, ...). Any screen
 * based on {@code AbstractRecipeBookScreen} - the player inventory, the
 * crafting table, furnace/blast furnace/smoker - overrides {@code render()}
 * and calls {@code super.renderContents(...)} directly instead of
 * {@code super.render(...)}, skipping {@code AbstractContainerScreen#render}
 * (and this injection) entirely. {@code renderContents} is the method both
 * paths actually share - it's also where {@code hoveredSlot} itself gets
 * refreshed - so injecting there instead covers every {@code AbstractContainerScreen}
 * subclass regardless of which one it is.
 *
 * <p>Phase 5r ("dark inventory") briefly added a rendering-based darkening
 * overlay plus a light-text override here - removed again after live testing
 * kept surfacing new rendering-order/shape/contrast edge cases (creative tab
 * sprite shapes, icon legibility, per-screen label overrides, ...). Replaced
 * with bundling an actual dark-themed resource pack ("Default Dark Mode",
 * see {@code launcher/resourcepacks-bundle/} and the license notes in
 * Projekt_Roadmap.md) - the same "let existing assets handle it instead of
 * reinventing rendering" call already made for 5f/5p, and a much better fit
 * here since the problem was fundamentally about texture/color, not logic.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	@Shadow
	private @Nullable Slot hoveredSlot;

	@Inject(method = "renderContents", at = @At("TAIL"))
	private void tntsallin1client$onRenderContentsTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ShulkerPreviewRenderer.capture(
				this.hoveredSlot != null && this.hoveredSlot.hasItem() ? this.hoveredSlot.getItem() : null,
				mouseX, mouseY);
	}
}
