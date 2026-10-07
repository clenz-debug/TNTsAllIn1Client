package com.tntsallin1client.mixin;

import java.util.List;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.screenshot.ScreenshotChatLink;
import com.tntsallin1client.tour.TourScreen;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Screenshot message: a click on its "[Copy]" link is ours to deal with - taken before the game
 * would send the link's command to the server (see {@link ScreenshotChatLink}). And themed screens
 * ({@link ThemedUi}) get the plain theme background in place of the game's (the darkened world, or
 * dirt), like the client design's title screen. And the in-game tour gets at the screen's buttons
 * through here.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin implements TourScreen {
	@Shadow
	@Final
	protected List<AbstractWidget> buttons;

	@Override
	public List<AbstractWidget> tnt$buttons() {
		return this.buttons;
	}

	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$themedBackground(int offset, CallbackInfo ci) {
		if (ThemedUi.isThemed((Screen) (Object) this)) {
			GuiComponent.fill(0, 0, this.width, this.height, ClientTheme.get().background1);
			ci.cancel();
		}
	}

	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$copyScreenshot(Component text, CallbackInfoReturnable<Boolean> cir) {
		if (text != null && ScreenshotChatLink.handleClick(text.getStyle().getClickEvent())) {
			cir.setReturnValue(true);
		}
	}
}
