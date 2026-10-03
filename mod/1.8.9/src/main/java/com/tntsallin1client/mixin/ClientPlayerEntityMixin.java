package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Freecam: keeps the player itself frozen while the camera is elsewhere. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	/**
	 * The player's whole per-tick update - movement, gravity, reading the movement keys, and at its
	 * end the packets telling the server where the player is and looks - is this one method. Skipping
	 * it leaves position and speed untouched and sends the server nothing about them.
	 */
	@Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
	private void tnt$freezeWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	/** The drop key in the world. Drops from an inventory screen are blocked in `ClientPlayerInteractionManagerMixin`. */
	@Inject(method = "dropSelectedItem(Z)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void tnt$blockDropWhileFreecam(boolean wholeStack, CallbackInfoReturnable<ItemEntity> cir) {
		if (FreecamHandler.isActive()) {
			cir.setReturnValue(null);
		}
	}
}
