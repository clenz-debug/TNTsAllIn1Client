package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam: an inventory can still be opened and sorted while it is active, but nothing may be
 * dropped out of it. (Attacking, mining, placing and using are stopped earlier, in
 * `MinecraftClientMixin`, before they get here.)
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
	/** The drop key over a slot. */
	private static final int THROW_MODE = 4;
	private static final int PICKUP_MODE = 0;
	private static final int QUICK_MOVE_MODE = 1;
	/** The slot number the game sends for a click outside the window. */
	private static final int OUTSIDE_WINDOW = -999;

	/**
	 * Cancelled before the game applies the click on its own side, so client and server stay in step.
	 * A click outside the window only drops in the two plain click modes - dragging items over slots
	 * reports its start and end with the same slot number.
	 */
	@Inject(method = "clickSlot(IIIILnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void tnt$blockInventoryDropWhileFreecam(int syncId, int slotId, int button, int mode, PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
		if (!FreecamHandler.isActive()) {
			return;
		}
		boolean dropsOutside = slotId == OUTSIDE_WINDOW && (mode == PICKUP_MODE || mode == QUICK_MOVE_MODE);
		if (mode == THROW_MODE || dropsOutside) {
			cir.setReturnValue(null);
		}
	}

	/** The creative inventory drops items through this method instead. */
	@Inject(method = "dropCreativeStack(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
	private void tnt$blockCreativeDropWhileFreecam(ItemStack stack, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}
}
