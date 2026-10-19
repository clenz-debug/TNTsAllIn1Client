package com.tntsallin1client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "3D items in inventory & hand": every item drawn in a screen or on the HUD - a flat place, see
 * {@link Items3d}. 1.19.x has no {@code GuiGraphics#renderItem}; the GUI item paths pick their model in
 * {@code tryRenderGuiItem} (every {@code renderAndDecorate*Item}) or in the public four-argument
 * {@code renderGuiItem} (checked with javap), so both are marked. The counter in {@link Items3d} allows
 * the nesting.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererGuiMixin {
	/** 1 / the item shader's ambient light (0.4), see {@code unlitFrontLit}. */
	@Unique
	private static final float UNLIT_FACTOR = 2.5f;

	@Unique
	private boolean tntsallin1client$unlit;

	@Inject(method = "tryRenderGuiItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("HEAD"))
	private void tntsallin1client$beginGuiItem(PoseStack pose, LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "tryRenderGuiItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("RETURN"))
	private void tntsallin1client$endGuiItem(PoseStack pose, LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}

	@Inject(method = "renderGuiItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	private void tntsallin1client$beginPlainGuiItem(PoseStack pose, ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "renderGuiItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
	private void tntsallin1client$endPlainGuiItem(PoseStack pose, ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}

	/**
	 * Items the pack lists as front-lit (the campfires) are drawn unlit here, the way 1.14.4 draws them:
	 * with every normal zero, the item shader's light comes out at its ambient part alone (0.4) on every
	 * face, and a colour factor of 2.5 for this one draw brings that back to the plain texture colour. The
	 * draw happens inside this method ({@code endBatch}), so the factor is reset at its end. Works for the
	 * game's own quad writer and Sodium's, which both take the normal from this pose without normalizing.
	 * User-confirmed in 1.19.4; 1.19.2 needed another way (there the factor never reached the draw).
	 */
	@ModifyArg(method = "renderGuiItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"), index = 3)
	private PoseStack tntsallin1client$unlitFrontLit(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay, BakedModel model) {
		if (Items3d.isFrontLit(stack)) {
			pose.last().normal().zero();
			RenderSystem.setShaderColor(UNLIT_FACTOR, UNLIT_FACTOR, UNLIT_FACTOR, 1.0f);
			this.tntsallin1client$unlit = true;
		}
		return pose;
	}

	@Inject(method = "renderGuiItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;IILnet/minecraft/client/resources/model/BakedModel;)V", at = @At("RETURN"))
	private void tntsallin1client$endUnlit(PoseStack pose, ItemStack stack, int x, int y, BakedModel model, CallbackInfo ci) {
		if (this.tntsallin1client$unlit) {
			this.tntsallin1client$unlit = false;
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}
}
