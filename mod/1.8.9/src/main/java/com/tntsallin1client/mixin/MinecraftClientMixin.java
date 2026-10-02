package com.tntsallin1client.mixin;

import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.menu.ClientMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reacts to our key bindings once per game tick. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	public Screen currentScreen;

	@Shadow
	public abstract void setScreen(Screen screen);

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void tnt$handleKeyBindings(CallbackInfo ci) {
		while (ModKeyBindings.OPEN_MENU.wasPressed()) {
			// Only from gameplay - with a screen open the key belongs to that screen.
			if (this.currentScreen == null) {
				this.setScreen(new ClientMenuScreen(null));
			}
		}
	}
}
