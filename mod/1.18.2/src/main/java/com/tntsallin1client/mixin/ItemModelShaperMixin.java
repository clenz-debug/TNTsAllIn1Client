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
 * "3D items in inventory & hand": every item's model is looked up here - in a flat place, with the
 * switch off, an item of the 3D blocks pack gets its flat copy instead. See {@link Items3d}.
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
