package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.inventory.ContainerClickPacing;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam: an inventory can still be opened and sorted while it is active, but nothing may be
 * dropped out of it. (Attacking, mining, placing and using are stopped earlier, in
 * `MinecraftMixin`, before they get here.) Also where the packet of every inventory click leaves -
 * it is handed to {@link ContainerClickPacing} instead.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	/** The slot number the game sends for a click outside the window. */
	private static final int OUTSIDE_WINDOW = -999;

	/**
	 * Cancelled before the game applies the click on its own side, so client and server stay in step.
	 * A click outside the window only drops in the two plain click modes - dragging items over slots
	 * reports its start and end with the same slot number.
	 */
	@Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockInventoryDropWhileFreecam(int containerId, int slotId, int button, ClickType type, Player player,
			CallbackInfoReturnable<ItemStack> cir) {
		if (!FreecamHandler.isActive()) {
			return;
		}
		boolean dropsOutside = slotId == OUTSIDE_WINDOW && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE);
		if (type == ClickType.THROW || dropsOutside) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}

	/**
	 * By the time the packet is sent the game has applied the click on its own side, so the player
	 * sees it at once; only telling the server is spread over the next ticks.
	 */
	@Redirect(method = "handleInventoryMouseClick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void tntsallin1client$paceClickPacket(ClientPacketListener connection, Packet<?> packet) {
		ContainerClickPacing.enqueue(connection, packet);
	}

	/** The creative inventory drops items through this method instead. */
	@Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockCreativeDropWhileFreecam(ItemStack stack, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}
}
