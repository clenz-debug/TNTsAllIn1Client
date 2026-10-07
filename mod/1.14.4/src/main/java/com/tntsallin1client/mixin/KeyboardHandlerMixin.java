package com.tntsallin1client.mixin;

import com.tntsallin1client.tour.InGameTour;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The in-game tour ({@link InGameTour}) is asked before the screen on display gets a key or a typed
 * character - while a step is shown, its screen gets neither.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	private static final int RELEASED = 0;
	private static final int PRESSED = 1;

	@Shadow
	@Final
	private Minecraft minecraft;

	/** A key let go always arrives - one held when the step began has to be released. */
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$keyPastTour(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
		if (window == this.minecraft.window.getWindow() && this.minecraft.screen != null && action != RELEASED
				&& !InGameTour.allowKey(this.minecraft.screen, key, action == PRESSED)) {
			ci.cancel();
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$charPastTour(long window, int codePoint, int modifiers, CallbackInfo ci) {
		if (window == this.minecraft.window.getWindow() && this.minecraft.screen != null && !InGameTour.allowChar(this.minecraft.screen)) {
			ci.cancel();
		}
	}
}
