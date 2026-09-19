package com.tntsallin1client.mixin;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5k rebuild, part 2/2: draws the pending shulker preview grid (if any,
 * captured by {@link AbstractContainerScreenMixin}) at the very end of
 * {@code extractRenderStateWithTooltipAndSubtitles} - after the vanilla item tooltip has already
 * been extracted, so our grid draws on top of it instead of being drawn over.
 */
@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
	private void tntsallin1client$onRenderWithTooltipAndSubtitles(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ShulkerPreviewRenderer.drawIfPending(guiGraphics);
	}
}
