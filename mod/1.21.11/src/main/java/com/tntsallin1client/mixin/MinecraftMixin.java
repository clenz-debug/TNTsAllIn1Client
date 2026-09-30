package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Blocks the swap-offhand key (F) in the world while freecam is active. Unlike the drop key it
 * doesn't go through a player method: {@code Minecraft#handleKeybinds} builds the
 * {@code SWAP_ITEM_WITH_OFFHAND} action packet and sends it itself - the only packet send in that
 * method (bytecode-verified), so skipping it there skips exactly the swap. The key press is still
 * consumed, so it doesn't fire belatedly once freecam ends.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private boolean tntsallin1client$blockOffhandSwapWhileFreecam(ClientPacketListener connection, Packet<?> packet) {
		return !FreecamHandler.isActive();
	}
}
