package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam: {@code GameRenderer} builds the first-person held-item/hand overlay from
 * {@code Minecraft.player} directly, not from whatever entity the camera is currently attached
 * to - so while freecam is active and the camera follows {@link com.tntsallin1client.freecam.FreecamEntity}
 * instead, this would otherwise render the real (frozen) player's hand/item floating statically
 * in view, not tracking the freecam's own look direction. Since interaction is already fully
 * locked while freecam is active (see {@link MultiPlayerGameModeMixin}), there's nothing useful
 * for it to show anyway - cancelled outright.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$hideDuringFreecam(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffer, LocalPlayer player, int packedLight, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	/**
	 * "3D items in inventory & hand": an item held in a hand, the own in first person and everyone's
	 * in third person (the layer on the player model draws through this method too) - a flat place,
	 * see {@link Items3d}. Items on heads, in frames or on the ground stay as they are.
	 */
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void tntsallin1client$beginHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext place, boolean leftHand,
			PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if (tntsallin1client$isHand(place)) {
			Items3d.beginFlatPlace();
		}
	}

	@Inject(method = "renderItem", at = @At("RETURN"))
	private void tntsallin1client$endHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext place, boolean leftHand,
			PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if (tntsallin1client$isHand(place)) {
			Items3d.endFlatPlace();
		}
	}

	@Unique
	private static boolean tntsallin1client$isHand(ItemDisplayContext place) {
		return place == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || place == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| place == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || place == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}
}
