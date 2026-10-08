package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No nametag above the player figure in the inventory while freecam is on (own user report).
 * Vanilla hides the own nametag only as long as the player is the camera entity
 * ({@code LivingEntityRenderer#shouldShowName}); in freecam the camera is the {@code FreecamEntity},
 * so the figure - drawn by the same renderer as the player in the world - got one.
 * {@code renderEntityInInventory} draws that figure for the survival and the creative inventory;
 * while it runs, {@link LivingEntityRendererMixin} answers "no name" for the own player. The nametag
 * above the frozen body in the world stays.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Inject(method = "renderEntityInInventory", at = @At("HEAD"))
	private static void tntsallin1client$beginFigure(GuiGraphics graphics, float x, float y, float scale, Vector3f translation, Quaternionf pose,
			Quaternionf cameraOrientation, LivingEntity entity, CallbackInfo ci) {
		FreecamHandler.drawingInventoryFigure = true;
	}

	@Inject(method = "renderEntityInInventory", at = @At("RETURN"))
	private static void tntsallin1client$endFigure(GuiGraphics graphics, float x, float y, float scale, Vector3f translation, Quaternionf pose,
			Quaternionf cameraOrientation, LivingEntity entity, CallbackInfo ci) {
		FreecamHandler.drawingInventoryFigure = false;
	}
}
