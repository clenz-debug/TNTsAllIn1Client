package com.tntsallin1client.mixin;

import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mod's "Client Mods" button on the title screen - the mod's settings are reachable before joining a world too. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void tntsallin1client$addClientModsButton(CallbackInfo ci) {
		this.addButton(MenuButtons.forTitleScreen(this, this.buttons));
	}
}
