package com.tntsallin1client.mixin;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
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
 *
 * <p>26.3 has two methods of this name: the one {@code addDetailsToTooltip} calls just passes on to
 * the one with a {@code TooltipProvider.Getter} (checked with javap), so that second one is the
 * target - named with its full descriptor, since a bare name would mean both.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(method = "addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/component/TooltipProvider$Getter;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
			at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onAddToTooltip(DataComponentType<?> type, TooltipProvider.Getter<?> getter, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {
		if (type == DataComponents.CONTAINER && ShulkerPreviewRenderer.hidesVanillaContents((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}
}
