package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5r follow-up (user report: "the crafting recipes button is still
 * light"): the recipe book toggle button (the small book icon next to
 * crafting-table/furnace/inventory screens) is a real {@code ImageButton}
 * widget added via {@code addRenderableWidget}, not part of {@code renderBg}'s
 * texture - so it's untouched by {@code AbstractContainerScreenMixin}'s panel
 * overlay, which only covers {@code renderBg}'s own bounds. It renders inside
 * {@code AbstractRecipeBookScreen#render} (an override that itself calls
 * {@code super.renderContents}/{@code renderBackground} rather than a plain
 * {@code super.render}, same general shape as 5k's render()-skipping quirk,
 * but this mixin targets the override directly so that doesn't matter here).
 *
 * <p>{@code getRecipeBookButtonPosition()} (the button's live x/y - it moves
 * depending on whether the recipe book panel itself is open) is {@code
 * protected abstract} directly on this mixin's own target class, so shadowing
 * it as an abstract stub is the same low-risk case as shadowing a field - no
 * hand-written bytecode target string, no per-subclass duplication of the
 * position math (unlike {@code CreativeModeInventoryScreenMixin}, which
 * needed to re-derive its formula because the private original lived on a
 * *different* class than the one being mixed here).
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	// Matches AbstractContainerScreenMixin's panel alpha (kept at the
	// stronger 0xB0 per the second round of feedback - see that class for
	// why). This button uses a plain rectangular sprite (no rounded/notched
	// corners like the creative tabs), so the flat fill here hasn't shown the
	// same "black box" edge artifact those did.
	private static final int DARK_OVERLAY_COLOR = 0xB0000000;
	private static final int BUTTON_WIDTH = 20;
	private static final int BUTTON_HEIGHT = 18;

	@Shadow
	protected abstract ScreenPosition getRecipeBookButtonPosition();

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!ClientConfig.get().darkInventoryEnabled) {
			return;
		}
		ScreenPosition pos = this.getRecipeBookButtonPosition();
		guiGraphics.fill(pos.x(), pos.y(), pos.x() + BUTTON_WIDTH, pos.y() + BUTTON_HEIGHT, DARK_OVERLAY_COLOR);
	}
}
