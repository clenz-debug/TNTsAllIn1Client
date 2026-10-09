package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * No nametag above the player figure in the inventory while freecam is on (own user report).
 * Vanilla hides the own nametag only as long as the player is the camera entity
 * ({@code LivingEntityRenderer#shouldShowName}); in freecam the camera is the {@code FreecamEntity},
 * so the figure - drawn through the same render state as the player in the world - got one.
 * {@code renderEntityInInventory} builds that state for the survival and the creative
 * inventory and hands it to {@code GuiGraphics#submitEntityRenderState}, and {@code PlayerRenderer#renderNameTag} draws exactly these two fields when they are
 * set (checked with javap). The nametag above the frozen body in the world stays.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@ModifyArg(
			method = "renderEntityInInventory",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;submitEntityRenderState(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V"),
			index = 0)
	private static EntityRenderState tntsallin1client$hideOwnNameTag(EntityRenderState state, @Local(argsOnly = true) LivingEntity entity) {
		if (entity == Minecraft.getInstance().player) {
			state.nameTag = null;
			if (state instanceof PlayerRenderState avatar) {
				avatar.scoreText = null;
			}
		}
		return state;
	}
}
