package com.tntsallin1client.mixin;

import com.tntsallin1client.itemphysics.ItemTilt;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item physics: the method the game positions a dropped item in (no name in this version's name
 * table) moves to the item's place - its first move - and then spins it. Both go through
 * {@link ItemTilt}, which leaves them as they are while the feature is off.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
	private static final String POSITION_ITEM = "method_10221(Lnet/minecraft/entity/ItemEntity;DDDFLnet/minecraft/client/render/model/BakedModel;)I";

	@Inject(method = POSITION_ITEM, at = @At("HEAD"))
	private void tnt$noteItem(ItemEntity entity, double x, double y, double z, float tickDelta, BakedModel model, CallbackInfoReturnable<Integer> cir) {
		ItemTilt.begin(entity, y, tickDelta, model);
	}

	@Redirect(method = POSITION_ITEM, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V", ordinal = 0))
	private void tnt$placeItem(float x, float y, float z) {
		ItemTilt.translate(x, y, z);
	}

	@Redirect(method = POSITION_ITEM, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotate(FFFF)V"))
	private void tnt$turnItem(float angle, float x, float y, float z) {
		ItemTilt.rotate(angle, x, y, z);
	}
}
