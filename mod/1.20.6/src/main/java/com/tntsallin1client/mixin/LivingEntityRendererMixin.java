package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The own player's figure in the inventory never carries a nametag - see {@link InventoryScreenMixin}. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$hideOwnNameTagInInventory(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (FreecamHandler.drawingInventoryFigure && entity == Minecraft.getInstance().player) {
			cir.setReturnValue(false);
		}
	}
}
