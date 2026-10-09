package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
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
	private void tntsallin1client$hideDuringFreecam(float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int packedLight, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}
}
