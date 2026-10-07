package com.tntsallin1client.mixin;

import java.util.List;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shulker box preview: this method adds nothing to a shulker box's tooltip but the list of its
 * first five contents ("Diamond x5 ... and 3 more") - left out while the feature is on, where the
 * preview panel shows all of them.
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
	@Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$hideContentLines(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
		if (ShulkerPreviewRenderer.hidesVanillaContents()) {
			ci.cancel();
		}
	}
}
