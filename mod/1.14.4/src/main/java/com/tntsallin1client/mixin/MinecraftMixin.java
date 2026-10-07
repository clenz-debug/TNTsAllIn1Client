package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Inventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** What the freecam keeps the player from doing. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** The hotbar keys (1-9) write the slot straight into the inventory - the only such write in `handleKeybinds`. */
	@Redirect(method = "handleKeybinds", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I", opcode = Opcodes.PUTFIELD))
	private void tntsallin1client$blockHotbarKeysWhileFreecam(Inventory inventory, int slot) {
		if (!FreecamHandler.isActive()) {
			inventory.selected = slot;
		}
	}

	/** The key that swaps the items of both hands tells the server right here - the only packet `handleKeybinds` sends. */
	@Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void tntsallin1client$blockHandSwapWhileFreecam(ClientPacketListener connection, Packet<?> packet) {
		if (!FreecamHandler.isActive()) {
			connection.send(packet);
		}
	}

	// Everything the player does to the world with the mouse starts in one of these four methods -
	// left click (attack, start mining; it also swings the arm, which the server is told), right
	// click (use, place), middle click (pick block) and the continued mining while the button is
	// held. Stopped here, nothing of it reaches the world or the server.

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockAttackWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockUseWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockPickWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockMiningWhileFreecam(boolean attacking, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	/**
	 * Escape while freecam is active ends freecam instead of opening the pause menu; pressed again,
	 * it opens the menu as usual. (The game also comes here when its window loses focus.)
	 */
	@Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$leaveFreecamInsteadOfPausing(boolean pauseOnly, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			FreecamHandler.exit((Minecraft) (Object) this);
			ci.cancel();
		}
	}
}
