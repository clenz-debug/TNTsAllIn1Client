package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "3D items in inventory & hand", see {@link Items3d}: notes while an item is drawn in the inventory (any slot on screen, the hotbar included). */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	private static final String RENDER_IN_GUI = "renderGuiItemModel(Lnet/minecraft/item/ItemStack;II)V";

	@Inject(method = RENDER_IN_GUI, at = @At("HEAD"))
	private void tnt$beginGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = RENDER_IN_GUI, at = @At("RETURN"))
	private void tnt$endGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}
}
