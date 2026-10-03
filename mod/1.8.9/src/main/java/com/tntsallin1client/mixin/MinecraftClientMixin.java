package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.ClientUserBadges;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.menu.ClientMenuScreen;
import net.minecraft.client.MinecraftClient;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** What happens once per game tick: our key bindings and the exchange with the launcher; also the mouse wheel while zooming. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	public Screen currentScreen;

	@Shadow
	public abstract void setScreen(Screen screen);

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void tnt$handleKeyBindings(CallbackInfo ci) {
		ClientUserBadges.tick((MinecraftClient) (Object) this);
		while (ModKeyBindings.OPEN_MENU.wasPressed()) {
			// Only from gameplay - with a screen open the key belongs to that screen.
			if (this.currentScreen == null) {
				this.setScreen(new ClientMenuScreen(null));
			}
		}
	}

	/** While zooming the mouse wheel changes the zoom level - the game must not switch the hotbar slot with it too. */
	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int tnt$zoomWithWheel() {
		return ZoomHandler.handleWheel(Mouse.getEventDWheel());
	}
}
