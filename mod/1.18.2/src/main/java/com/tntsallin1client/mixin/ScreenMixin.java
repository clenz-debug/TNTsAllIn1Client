package com.tntsallin1client.mixin;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase 5k rebuild, part 2/2 - the pending shulker preview grid ({@link ShulkerPreviewRenderer}, captured
 * by {@link AbstractContainerScreenMixin}) - lives in {@code GameRendererScreenMixin} in 1.18.2: there is
 * no {@code renderWithTooltip} to draw it at the end of.
 *
 * <p>Phase 5n: catches clicks on the "[Copy]" screenshot chat link before the game
 * would send its stand-in command to the server.
 */
@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$copyScreenshot(@Nullable Style style, CallbackInfoReturnable<Boolean> cir) {
		if (style != null && ScreenshotChatLink.handleClick(style.getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
