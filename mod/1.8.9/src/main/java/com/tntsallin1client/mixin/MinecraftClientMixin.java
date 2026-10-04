package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.friends.ActivityReporter;
import com.tntsallin1client.friends.ClientUserBadges;
import com.tntsallin1client.friends.FriendsBridge;
import com.tntsallin1client.debug.SystemInfoHud;
import com.tntsallin1client.discord.DiscordPresenceManager;
import com.tntsallin1client.inventory.ContainerClickPacing;
import com.tntsallin1client.screenshot.ScreenshotWatcher;
import org.lwjgl.input.Keyboard;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.tour.InGameTour;
import com.tntsallin1client.menu.WaypointMenuIntegration;
import com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer;
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

/**
 * What happens once per game tick: our key bindings, the exchange with the launcher and the freecam;
 * also the mouse wheel while zooming, and what the freecam keeps the player from doing.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	public Screen currentScreen;

	@Shadow
	public abstract void setScreen(Screen screen);

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void tnt$handleKeyBindings(CallbackInfo ci) {
		ClientUserBadges.tick((MinecraftClient) (Object) this);
		FreecamHandler.tick((MinecraftClient) (Object) this);
		WaypointMenuIntegration.tick((MinecraftClient) (Object) this);
		SpawnOverlayRenderer.tick((MinecraftClient) (Object) this);
		ContainerClickPacing.tick((MinecraftClient) (Object) this);
		ScreenshotWatcher.tick((MinecraftClient) (Object) this);
		ActivityReporter.tick((MinecraftClient) (Object) this);
		FriendsBridge.tick((MinecraftClient) (Object) this);
		DiscordPresenceManager.tick((MinecraftClient) (Object) this);
		InGameTour.tick((MinecraftClient) (Object) this);
		while (ModKeyBindings.SYSTEM_INFO.wasPressed()) {
			// Only together with F3, like the game's own debug key combinations.
			if (Keyboard.isKeyDown(Keyboard.KEY_F3)) {
				SystemInfoHud.visible = !SystemInfoHud.visible;
			}
		}
		while (ModKeyBindings.OPEN_MENU.wasPressed()) {
			// Only from gameplay - with a screen open the key belongs to that screen.
			if (this.currentScreen == null) {
				this.setScreen(ClientMenus.create(null));
			}
		}
	}

	/** While zooming the mouse wheel changes the zoom level - the game must not switch the hotbar slot with it too. */
	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int tnt$zoomWithWheel() {
		return ZoomHandler.handleWheel(Mouse.getEventDWheel());
	}

	// Freecam: everything the player does to the world with the mouse starts in one of these four
	// methods - left click (attack, start mining; it also swings the arm, which the server is told),
	// right click (use, place), middle click (pick block) and the continued mining while the button
	// is held. Stopped here, nothing of it reaches the world or the server.

	@Inject(method = "doAttack()V", at = @At("HEAD"), cancellable = true)
	private void tnt$blockAttackWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "doUse()V", at = @At("HEAD"), cancellable = true)
	private void tnt$blockUseWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "doPick()V", at = @At("HEAD"), cancellable = true)
	private void tnt$blockPickWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "handleBlockBreaking(Z)V", at = @At("HEAD"), cancellable = true)
	private void tnt$blockMiningWhileFreecam(boolean breaking, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	/**
	 * Escape while freecam is active ends freecam instead of opening the pause menu; pressed again,
	 * it opens the menu as usual. (The game also comes here when its window loses focus.)
	 */
	@Inject(method = "openGameMenuScreen()V", at = @At("HEAD"), cancellable = true)
	private void tnt$leaveFreecamInsteadOfPausing(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			FreecamHandler.exit((MinecraftClient) (Object) this);
			ci.cancel();
		}
	}
}
