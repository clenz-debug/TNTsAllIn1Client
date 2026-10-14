package com.tntsallin1client.mixin;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase 5k rebuild, part 2/2: draws the pending shulker preview grid (if any,
 * captured by {@link AbstractContainerScreenMixin}) at the very end of
 * {@code renderWithTooltip} - after the vanilla item tooltip, so our grid draws
 * on top of it instead of being drawn over.
 *
 * <p>Phase 5n: also catches clicks on the "[Copy]" screenshot chat link before the game
 * would send its stand-in command to the server.
 */
@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(method = "renderWithTooltip", at = @At("TAIL"))
	private void tntsallin1client$onRenderWithTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ShulkerPreviewRenderer.drawIfPending(guiGraphics);
	}

	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$copyScreenshot(@Nullable Style style, CallbackInfoReturnable<Boolean> cir) {
		if (style != null && ScreenshotChatLink.handleClick(style.getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
