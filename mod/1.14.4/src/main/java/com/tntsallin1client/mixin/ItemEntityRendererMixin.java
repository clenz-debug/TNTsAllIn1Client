package com.tntsallin1client.mixin;

import com.tntsallin1client.itemphysics.ItemTilt;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item physics: the method the game positions a dropped item in moves to the item's place - its
 * one move - and then spins it (bytecode-checked). Both go through {@link ItemTilt}, which leaves
 * them as they are while the feature is off.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
	@Inject(method = "setupBobbingItem", at = @At("HEAD"))
	private void tntsallin1client$noteItem(ItemEntity entity, double x, double y, double z, float partialTick, BakedModel model,
			CallbackInfoReturnable<Integer> cir) {
		ItemTilt.begin(entity, y, partialTick, model);
	}

	@Redirect(method = "setupBobbingItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;translatef(FFF)V"))
	private void tntsallin1client$placeItem(float x, float y, float z) {
		ItemTilt.translate(x, y, z);
	}

	@Redirect(method = "setupBobbingItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotatef(FFFF)V"))
	private void tntsallin1client$turnItem(float angle, float x, float y, float z) {
		ItemTilt.rotate(angle, x, y, z);
	}
}
