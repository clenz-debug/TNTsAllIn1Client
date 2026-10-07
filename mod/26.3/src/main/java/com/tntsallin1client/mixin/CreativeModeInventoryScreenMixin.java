package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam: the creative inventory handles its clicks itself and tells the server the resulting
 * slot contents, so the block in {@link MultiPlayerGameModeMixin} never sees them. The same clicks
 * are blocked here, before anything changes on the client: whatever drops an item or moves one
 * into or out of the offhand.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockDropAndOffhandWhileFreecam(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!FreecamHandler.isActive() || player == null) {
			return;
		}
		boolean drops = input == ContainerInput.THROW
				|| (input == ContainerInput.PICKUP && slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE);
		// The inventory tab wraps the player's own slots and numbers them like the inventory menu
		// does (the wrapper's constructor, bytecode-verified) - the offhand is that menu's shield slot.
		boolean touchesOffhand = (input == ContainerInput.SWAP && button == Inventory.SLOT_OFFHAND)
				|| (slot != null && slot.container instanceof Inventory && slot.getContainerSlot() == InventoryMenu.SHIELD_SLOT)
				|| (input == ContainerInput.QUICK_MOVE && slot != null && slot.container instanceof Inventory && slot.hasItem()
						&& player.getEquipmentSlotForItem(slot.getItem()) == EquipmentSlot.OFFHAND);
		if (drops || touchesOffhand) {
			ci.cancel();
		}
	}
}
