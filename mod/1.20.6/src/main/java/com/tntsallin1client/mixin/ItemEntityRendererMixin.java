package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5o: purely cosmetic "item physics" for dropped items. Deliberately
 * does NOT attempt real physics (items rolling to a stop, sliding, not
 * clipping through blocks): that needs the actual entity position/orientation
 * to be correct, which the server simulates and syncs - a client-only mod can
 * only affect rendering, not real position.
 *
 * <p>What IS honestly achievable client-only, and what this does: stop the
 * vanilla spin everywhere except in a fluid, using a fixed per-item angle
 * derived from {@code bobOffs} (stable - it never changes over time, so
 * "not rotating" actually means that, not just "rotating slower"). Once the
 * item is resting on solid ground it additionally lays flat instead of
 * upright, at that same fixed angle - while still falling/thrown it keeps
 * the frozen angle but stays upright, per explicit user feedback that even
 * mid-air spin (matching vanilla's own resting-item spin) looked wrong. Only
 * in a fluid does this fall back to plain vanilla rendering (bob + spin) -
 * vanilla's own fluid buoyancy already makes it float/bob at the surface
 * correctly, and freezing the angle there would fight that.
 *
 * <p>Cancels the whole method and reimplements it (the same steps as the
 * game's own, bytecode-read, minus bob and spin) - but only when the resting
 * pose actually applies; otherwise this doesn't cancel at all and vanilla's
 * own {@code render} runs unmodified. In 1.20.6 the renderer still gets the
 * entity itself, so no render state has to carry "on ground"/"in water".
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity> {
	@Shadow
	@Final
	private RandomSource random;

	@Shadow
	@Final
	private ItemRenderer itemRenderer;

	protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRender(ItemEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
			int packedLight, CallbackInfo ci) {
		ItemStack stack = entity.getItem();
		if (!ClientConfig.get().itemTiltEnabled || stack.isEmpty()) {
			return;
		}
		if (entity.isInWater()) {
			// Floating - leave vanilla's own bob/spin (and its correct fluid buoyancy) alone.
			return;
		}

		poseStack.pushPose();
		this.random.setSeed(ItemEntityRenderer.getSeedForItemStack(stack));
		BakedModel model = this.itemRenderer.getModel(stack, entity.level(), null, entity.getId());
		// Fixed per-item yaw so a pile of items doesn't all lie at the exact same angle -
		// derived from bobOffs alone (not the item's age), so unlike vanilla's spin it never
		// changes frame to frame. Applied whether the item is still falling/thrown or
		// already resting, so it never visibly spins at any point.
		float restYaw = entity.bobOffs * ((float) Math.PI * 2.0F);
		if (entity.onGround()) {
			// Vanilla's own height above the ground at the lowest point of its bobbing.
			float lift = 0.25F * model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
			poseStack.translate(0.0F, lift, 0.0F);
			poseStack.mulPose(Axis.YP.rotation(restYaw));
			// Lay the normally-upright item flat, face up, instead of standing on its edge.
			poseStack.mulPose(Axis.XP.rotation((float) (Math.PI / 2.0)));
		} else {
			// Still moving - keep it upright (no ground-relative translate, no flat lay,
			// which would look wrong on something that isn't actually resting on anything),
			// just without the continuous vanilla spin.
			poseStack.mulPose(Axis.YP.rotation(restYaw));
		}
		ItemEntityRenderer.renderMultipleFromCount(this.itemRenderer, poseStack, buffer, packedLight, stack, model, model.isGui3d(), this.random);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
		ci.cancel();
	}
}
