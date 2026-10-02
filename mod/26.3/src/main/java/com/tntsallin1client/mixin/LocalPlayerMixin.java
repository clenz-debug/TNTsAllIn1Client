package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freezes the real player while freecam is active by canceling its whole per-tick update.
 * {@code LocalPlayer#tick()} contains, in order: {@code super.tick()} (the entire physics/
 * gravity/travel/input-sampling chain, all the way up through {@code Entity}) and then directly
 * the outbound {@code ServerboundPlayerInputPacket}/{@code ServerboundMovePlayerPacket.Rot}/
 * {@code ServerboundMoveVehiclePacket} sends plus {@code sendPosition()} - every movement-related
 * packet the client sends per tick lives in this one method (verified via bytecode). Canceling it
 * entirely is therefore the simplest way to guarantee position, velocity, gravity and outbound
 * movement packets all stay completely untouched for as long as freecam is active - which is
 * exactly what "the server sees nothing happen" requires. See
 * {@link com.tntsallin1client.mixin.EntityTurnMixin} for the matching rotation freeze.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$freezeWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}
}
