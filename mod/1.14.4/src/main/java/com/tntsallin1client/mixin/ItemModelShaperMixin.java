package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "3D items in inventory & hand" (see {@link Items3d}): every lookup of an item's model ends up
 * here - while the inventory or a hand is being drawn and the switch is off, an item our 3D pack
 * changes gets the model the game has for it without the pack.
 */
@Mixin(ItemModelShaper.class)
public abstract class ItemModelShaperMixin {
	@Inject(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;",
			at = @At("RETURN"), cancellable = true)
	private void tntsallin1client$flatItemModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
		BakedModel model = cir.getReturnValue();
		BakedModel shown = Items3d.modelFor(stack, model);
		if (shown != model) {
			cir.setReturnValue(shown);
		}
	}
}
