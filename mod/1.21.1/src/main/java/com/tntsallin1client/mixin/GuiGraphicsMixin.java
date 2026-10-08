package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.DarkModePack;
import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Light container labels while our dark mode pack is active, see {@link DarkModePack}. Every other
 * {@code drawString}/{@code drawCenteredString}/{@code drawWordWrap} variant ends in one of these two methods
 * (checked with javap), so all GUI text passes through them. Third int = the colour.
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
	@ModifyVariable(
			method = {
					"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
					"drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"},
			at = @At("HEAD"),
			ordinal = 2,
			argsOnly = true)
	private int tntsallin1client$darkModeLabelColor(int color) {
		return DarkModePack.recolorLabel(color);
	}

	/**
	 * "3D items in inventory & hand": every item drawn in a screen or on the HUD ends in this one
	 * {@code renderItem} (checked with javap) - a flat place, see {@link Items3d}.
	 */
	@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("HEAD"))
	private void tntsallin1client$beginGuiItem(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("RETURN"))
	private void tntsallin1client$endGuiItem(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}
}
