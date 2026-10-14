package com.tntsallin1client.mixin;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase 5n rebuild: handles clicks on the "[Copy]" screenshot chat link
 * before vanilla's own {@code handleComponentClicked} would send its stand-in
 * command to the server - see {@link ScreenshotChatLink}.
 */
@Mixin(Screen.class)
public class ChatScreenMixin {
	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onComponentClicked(@Nullable Style style, CallbackInfoReturnable<Boolean> cir) {
		if (style != null && ScreenshotChatLink.handleClick(style.getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
