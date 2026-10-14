package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "3D items in inventory & hand": an item held in a hand in third person (everyone's, the own one
 * too). Since 1.21.2 an entity's held items get their model while its render state is filled, through
 * this method - a flat place for the two hand places, see {@link Items3d}. The head stays as it is.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	@Inject(method = "resolveItemModel", at = @At("HEAD"))
	private void tntsallin1client$beginHeldItem(ItemStack stack, LivingEntity entity, ItemDisplayContext place,
			CallbackInfoReturnable<BakedModel> cir) {
		if (tntsallin1client$isHand(place)) {
			Items3d.beginFlatPlace();
		}
	}

	@Inject(method = "resolveItemModel", at = @At("RETURN"))
	private void tntsallin1client$endHeldItem(ItemStack stack, LivingEntity entity, ItemDisplayContext place,
			CallbackInfoReturnable<BakedModel> cir) {
		if (tntsallin1client$isHand(place)) {
			Items3d.endFlatPlace();
		}
	}

	@Unique
	private static boolean tntsallin1client$isHand(ItemDisplayContext place) {
		return place == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || place == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}
}
