package com.tntsallin1client.mixin;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Leaves the vanilla shulker box content lines ("Diamond x5", "and 3 more...") out of the
 * tooltip while the shulker preview feature is on - the preview grid replaces them. In this version
 * they come from the box block's own {@code appendHoverText}, behind a check whether the item's block
 * entity data holds an item list (its second {@code contains} call, the first asks for a loot table);
 * that check answers "no" here, so the "???????" line of an unopened loot box stays.
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
	@Redirect(method = "appendHoverText", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;I)Z"))
	private boolean tntsallin1client$hideContentLines(CompoundTag tag, String key, int type, ItemStack stack) {
		return !ShulkerPreviewRenderer.hidesVanillaContents(stack) && tag.contains(key, type);
	}
}
