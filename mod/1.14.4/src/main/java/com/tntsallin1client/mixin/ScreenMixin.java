package com.tntsallin1client.mixin;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Screenshot message: a click on its "[Copy]" link is ours to deal with - taken before the game
 * would send the link's command to the server (see {@link ScreenshotChatLink}).
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$copyScreenshot(Component text, CallbackInfoReturnable<Boolean> cir) {
		if (text != null && ScreenshotChatLink.handleClick(text.getStyle().getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
