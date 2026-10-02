package com.tntsallin1client.mixin;

import com.tntsallin1client.menu.ClientMenuScreen;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The "Client Mods" button in the pause menu. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	@Inject(method = "init()V", at = @At("RETURN"))
	private void tnt$addClientModsButton(CallbackInfo ci) {
		MenuButtons.addToPauseMenu(this.buttons);
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"))
	private void tnt$openClientMods(ButtonWidget button, CallbackInfo ci) {
		if (button.id == MenuButtons.OPEN_MENU_BUTTON_ID) {
			this.client.setScreen(new ClientMenuScreen(this));
		}
	}
}
