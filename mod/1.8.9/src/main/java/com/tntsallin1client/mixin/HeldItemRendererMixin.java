package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "3D items in inventory & hand", see {@link Items3d}: notes while an item is drawn in a hand - the
 * player's own in first person, anyone's in third person. The same method draws an item worn on the
 * head, which stays 3D.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
	private static final String RENDER_ITEM = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V";

	@Inject(method = RENDER_ITEM, at = @At("HEAD"))
	private void tnt$beginHeldItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode mode, CallbackInfo ci) {
		if (tnt$isHand(mode)) {
			Items3d.beginFlatPlace();
		}
	}

	@Inject(method = RENDER_ITEM, at = @At("RETURN"))
	private void tnt$endHeldItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode mode, CallbackInfo ci) {
		if (tnt$isHand(mode)) {
			Items3d.endFlatPlace();
		}
	}

	private static boolean tnt$isHand(ModelTransformation.Mode mode) {
		return mode == ModelTransformation.Mode.FIRST_PERSON || mode == ModelTransformation.Mode.THIRD_PERSON;
	}
}
