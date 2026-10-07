package com.tntsallin1client.mixin;

import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mod's "Client Mods" button in the pause menu. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void tntsallin1client$addClientModsButton(CallbackInfo ci) {
		Button button = MenuButtons.forPauseMenu(this, this.buttons);
		if (button != null) {
			this.addButton(button);
		}
	}
}
