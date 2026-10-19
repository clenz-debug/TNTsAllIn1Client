package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "3D items in inventory & hand": every item drawn in a screen or on the HUD - a flat place, see
 * {@link Items3d}. 1.19.x has no {@code GuiGraphics#renderItem}; the GUI item paths pick their model in
 * {@code tryRenderGuiItem} (every {@code renderAndDecorate*Item}) or in the public three-argument
 * {@code renderGuiItem} (checked with javap), so both are marked. The counter in {@link Items3d} allows
 * the nesting.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererGuiMixin {
	/** Toward the viewer in the inventory - where a flat item's front faces. */
	@Unique
	private static final Vector3f TOWARD_VIEWER = new Vector3f(0.0f, 0.0f, 1.0f);

	@Unique
	private boolean tntsallin1client$unlit;

	@Inject(method = "tryRenderGuiItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("HEAD"))
	private void tntsallin1client$beginGuiItem(LivingEntity entity, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "tryRenderGuiItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("RETURN"))
	private void tntsallin1client$endGuiItem(LivingEntity entity, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}

	@Inject(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	private void tntsallin1client$beginPlainGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.beginFlatPlace();
	}

	@Inject(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
	private void tntsallin1client$endPlainGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		Items3d.endFlatPlace();
	}

	/**
	 * Items the pack lists as front-lit (the campfires) get both of the inventory's lights pointed
	 * straight at the viewer for their draw: every face turned towards the viewer by 30 degrees or more
	 * - all the faces this icon shows - is then fully lit, the way 1.14.4 draws them unlit. The pack's
	 * own front light ({@code gui_light}) suits the newest versions but left the tilted 3D icon very dark
	 * here (own user report, twice: a first try with zeroed normals and a colour factor kept only the
	 * ambient light). The method sets the inventory's normal lights again at its end anyway for such a
	 * model; {@code endUnlit} makes sure of it.
	 */
	@ModifyArg(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemTransforms$TransformType;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"), index = 3)
	private PoseStack tntsallin1client$unlitFrontLit(ItemStack stack, ItemTransforms.TransformType context, boolean leftHand, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay, BakedModel model) {
		if (Items3d.isFrontLit(stack)) {
			RenderSystem.setShaderLights(TOWARD_VIEWER, TOWARD_VIEWER);
			this.tntsallin1client$unlit = true;
		}
		return pose;
	}

	@Inject(method = "renderGuiItem(Lnet/minecraft/world/item/ItemStack;IILnet/minecraft/client/resources/model/BakedModel;)V", at = @At("RETURN"))
	private void tntsallin1client$endUnlit(ItemStack stack, int x, int y, BakedModel model, CallbackInfo ci) {
		if (this.tntsallin1client$unlit) {
			this.tntsallin1client$unlit = false;
			Lighting.setupFor3DItems();
		}
	}
}
