package com.tntsallin1client.mixin;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves the vanilla shulker box content lines ("Diamond x5", "and 3 more...") out of the
 * tooltip while the shulker preview feature is on - the preview grid replaces them. Those lines
 * come from {@code ItemContainerContents#addToTooltip}, which {@code addDetailsToTooltip} reaches
 * through this method with {@link DataComponents#CONTAINER}; that provider itself doesn't know
 * which item it belongs to, so the shulker-box check has to happen here. Other containers
 * (e.g. a chest item carrying block entity data) keep their vanilla lines.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onAddToTooltip(DataComponentType<?> type, Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {
		if (type == DataComponents.CONTAINER && ShulkerPreviewRenderer.hidesVanillaContents((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}
}
