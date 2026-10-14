package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Inventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam: what the keys in the world must not do to the frozen player's hands while it is active -
 * swap the offhand, switch the hotbar slot, pick a block. The mouse wheel's hotbar switch is
 * blocked in {@link MouseHandlerMixin}.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	/**
	 * Blocks the swap-offhand key (F). Unlike the drop key it doesn't go through a player method:
	 * {@code Minecraft#handleKeybinds} builds the {@code SWAP_ITEM_WITH_OFFHAND} action packet and
	 * sends it itself - the only packet send in that method (bytecode-verified), so skipping it
	 * there skips exactly the swap. The key press is still consumed, so it doesn't fire belatedly
	 * once freecam ends.
	 */
	@WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private boolean tntsallin1client$blockOffhandSwapWhileFreecam(ClientPacketListener connection, Packet<?> packet) {
		return !FreecamHandler.isActive();
	}

	/**
	 * Blocks the hotbar keys (1-9): the only write to {@code Inventory#selected} in
	 * {@code Minecraft#handleKeybinds} (bytecode-verified). Blocked at the write rather than in
	 * {@code Inventory} itself, so a slot change the server orders still goes through.
	 */
	@WrapWithCondition(method = "handleKeybinds", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
			target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
	private boolean tntsallin1client$blockHotbarKeysWhileFreecam(Inventory inventory, int slot) {
		return !FreecamHandler.isActive();
	}

	/** Blocks pick block (middle click): it asks the server to put the item into the hand, switching the hotbar slot. */
	@Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockPickWhileFreecam(CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}
}
