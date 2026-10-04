package com.tntsallin1client.mixin;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicks on chat text end up here - the "[Copy]" link of a screenshot message is ours and never goes to the server. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "handleTextClick(Lnet/minecraft/text/Text;)Z", at = @At("HEAD"), cancellable = true)
	private void tnt$copyScreenshot(Text text, CallbackInfoReturnable<Boolean> cir) {
		if (text != null && ScreenshotChatLink.handleClick(text.getStyle().getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
