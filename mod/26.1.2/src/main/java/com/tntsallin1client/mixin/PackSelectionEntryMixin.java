package com.tntsallin1client.mixin;

import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's move arrows only check whether the *neighbour* is fixed, so a fixed pack sitting right
 * below a movable one - exactly where our bundled packs live (see {@code BundledResourcePacks}) -
 * could still be moved up out of its place. A fixed pack just never moves.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.packs.PackSelectionModel$EntryBase")
public class PackSelectionEntryMixin {
	@Shadow
	@Final
	private Pack pack;

	@Inject(method = {"canMoveUp", "canMoveDown"}, at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$fixedPacksNeverMove(CallbackInfoReturnable<Boolean> cir) {
		if (this.pack.isFixedPosition()) {
			cir.setReturnValue(false);
		}
	}
}
