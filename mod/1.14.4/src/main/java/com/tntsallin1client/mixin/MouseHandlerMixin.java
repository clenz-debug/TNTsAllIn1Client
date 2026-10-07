package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Zoom: the mouse turns the player more slowly while zooming, and the wheel changes the zoom level.
 * Freecam: the mouse turns the camera instead of the player, and the wheel leaves the hotbar alone.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Redirect(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;sensitivity:D"))
	private double tntsallin1client$zoomSensitivity(Options options) {
		return ZoomHandler.sensitivity(options.sensitivity);
	}

	/**
	 * Freecam: this is where the game turns the player by what the mouse moved since the last frame.
	 * While freecam is active the camera is turned instead and the player keeps looking where it looked.
	 */
	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void tntsallin1client$turnFreecamInsteadOfPlayer(LocalPlayer player, double yawChange, double pitchChange) {
		if (FreecamHandler.isActive()) {
			FreecamHandler.turn(yawChange, pitchChange);
		} else {
			player.turn(yawChange, pitchChange);
		}
	}

	/**
	 * Where the wheel switches the hotbar slot during the game (screens and the spectator menu get
	 * the wheel before this). While zooming it changes the zoom level and must not do both; while
	 * freecam is active the frozen player keeps the item in its hand.
	 */
	@Redirect(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"))
	private void tntsallin1client$zoomWithWheel(Inventory inventory, double amount) {
		if (!ZoomHandler.handleWheel(amount) && !FreecamHandler.isActive()) {
			inventory.swapPaint(amount);
		}
	}
}
