package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import java.util.List;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import com.tntsallin1client.tour.InGameTour;
import com.tntsallin1client.tour.TourScreen;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clicks on chat text end up here - the "[Copy]" link of a screenshot message is ours and never goes
 * to the server. And themed screens ({@link ThemedUi}) get the plain theme background in place of
 * the game's (the darkened world, or dirt), like the client design's title screen. And the in-game tour
 * ({@link InGameTour}) is asked before the screen gets a mouse or keyboard event, and gets at the
 * screen's buttons through here.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin implements TourScreen {
	@Shadow
	protected List<ButtonWidget> buttons;

	@Override
	public List<ButtonWidget> tnt$buttons() {
		return this.buttons;
	}

	@Redirect(method = "handleInput()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;handleMouse()V"))
	private void tnt$mouseEventPastTour(Screen screen) {
		if (InGameTour.allowMouse(screen)) {
			screen.handleMouse();
		}
	}

	@Redirect(method = "handleInput()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;handleKeyboard()V"))
	private void tnt$keyEventPastTour(Screen screen) {
		if (InGameTour.allowKey(screen)) {
			screen.handleKeyboard();
		}
	}

	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
	private void tnt$themedBackground(int alpha, CallbackInfo ci) {
		if (ThemedUi.isThemed((Screen) (Object) this)) {
			DrawableHelper.fill(0, 0, this.width, this.height, ClientTheme.get().background1);
			ci.cancel();
		}
	}

	@Inject(method = "handleTextClick(Lnet/minecraft/text/Text;)Z", at = @At("HEAD"), cancellable = true)
	private void tnt$copyScreenshot(Text text, CallbackInfoReturnable<Boolean> cir) {
		if (text != null && ScreenshotChatLink.handleClick(text.getStyle().getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
