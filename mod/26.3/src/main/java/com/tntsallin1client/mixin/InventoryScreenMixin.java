package com.tntsallin1client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No nametag above the player figure in the inventory while freecam is on (own user report).
 * Vanilla hides the own nametag only as long as the player is the camera entity
 * ({@code LivingEntityRenderer#shouldShowName}); in freecam the camera is the {@code FreecamEntity},
 * so the figure - drawn through the same render state as the player in the world - got one.
 * {@code extractRenderState(LivingEntity)} builds that state for the survival and the creative
 * inventory, and {@code EntityRenderer#submitNameDisplay} draws exactly these two fields when they
 * are set (checked with javap). The nametag above the frozen body in the world stays.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
			at = @At("RETURN"))
	private static void tntsallin1client$hideOwnNameTag(LivingEntity entity, CallbackInfoReturnable<EntityRenderState> cir) {
		if (entity == Minecraft.getInstance().player) {
			EntityRenderState state = cir.getReturnValue();
			state.nameTag = null;
			state.scoreText = null;
		}
	}
}
