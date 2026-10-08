package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "3D items in inventory & hand" (see {@link Items3d}): tells it while an item is being drawn in
 * the inventory (or any other screen, the hotbar included) or in a hand, first or third person -
 * the places that show the item without our 3D model while the switch is off. Also lights the
 * few 3D items the pack names for it from the front in the inventory.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	@Inject(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	private void tntsallin1client$beginGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
	private void tntsallin1client$endGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}

	/**
	 * Where the inventory decides how an item is lit: a 3D model from the side, a flat one not at all.
	 * The items the 3D pack names for it are drawn the second way although they are 3D models.
	 */
	@Redirect(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;isGui3d()Z"))
	private boolean tntsallin1client$frontLitInInventory(BakedModel model, ItemStack stack, int x, int y, BakedModel sameModel) {
		return model.isGui3d() && !Items3d.isFrontLit(stack);
	}

	@Inject(method = "renderAndDecorateItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	private void tntsallin1client$beginSlotItem(LivingEntity entity, ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "renderAndDecorateItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
	private void tntsallin1client$endSlotItem(LivingEntity entity, ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}

	@Inject(method = "renderWithMobState", at = @At("HEAD"))
	private void tntsallin1client$beginHeldItem(ItemStack stack, LivingEntity entity, ItemTransforms.TransformType place, boolean leftHand, CallbackInfo ci) {
		if (tntsallin1client$isHand(place)) {
			Items3d.beginFlatPlace();
		}
	}

	@Inject(method = "renderWithMobState", at = @At("RETURN"))
	private void tntsallin1client$endHeldItem(ItemStack stack, LivingEntity entity, ItemTransforms.TransformType place, boolean leftHand, CallbackInfo ci) {
		if (tntsallin1client$isHand(place)) {
			Items3d.endFlatPlace();
		}
	}

	private static boolean tntsallin1client$isHand(ItemTransforms.TransformType place) {
		return place == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND || place == ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
				|| place == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND || place == ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND;
	}
}
