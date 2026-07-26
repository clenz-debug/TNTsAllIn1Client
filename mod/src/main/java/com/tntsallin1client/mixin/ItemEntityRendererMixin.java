package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5o: purely cosmetic "item physics" - dropped items lean at a small,
 * per-item-stable angle instead of standing perfectly upright while they spin
 * and bob (both of which are vanilla behavior already, see
 * {@code ItemEntityRenderer#submit}). Deliberately does NOT attempt real
 * physics (items settling flat, rolling, not clipping through blocks): that
 * needs the actual entity position/orientation to be correct, which the
 * server simulates and syncs - a client-only mod can only affect rendering,
 * not real position, so it would either look wrong against the server's
 * authoritative state or require the server to run the same mod too. That's
 * exactly the trade-off in the well-known "ItemPhysic" mod (LGPL-2.1,
 * checked on Modrinth): Modrinth lists it as server_side: required, not just
 * client. Bundling that would silently stop doing anything real the moment
 * the user joins a server that doesn't also run it - i.e. almost every
 * server, including this project's own explicit "works on any
 * vanilla-compatible server, like Lunar/Badlion" premise - so instead of
 * bundling a mod that only half-works here, this reimplements the tiny
 * purely-visual part that IS honestly achievable client-only: a fixed lean
 * angle, derived from the same per-entity {@code bobOffset} vanilla itself
 * already uses for bob/spin phase (stable across frames, no new state to
 * track).
 *
 * <p>No clean single-call extension point to add one extra rotation to an
 * existing PoseStack transform sequence without a hand-written bytecode
 * INVOKE target string (wrong by even one character and the mixin fails to
 * apply at game startup, not just "doesn't work" - unverifiable without
 * actually launching the game). Cancels the whole method instead and
 * reimplements it, same trade-off already accepted for 5l's hitbox color.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
	@Shadow
	@Final
	private RandomSource random;

	protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onSubmit(
			ItemEntityRenderState itemEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraRenderState, CallbackInfo ci) {
		if (!ClientConfig.get().itemTiltEnabled || itemEntityRenderState.item.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		AABB aabb = itemEntityRenderState.item.getModelBoundingBox();
		float f = -((float) aabb.minY) + 0.0625F;
		float g = Mth.sin(itemEntityRenderState.ageInTicks / 10.0F + itemEntityRenderState.bobOffset) * 0.1F + 0.1F;
		poseStack.translate(0.0F, g + f, 0.0F);
		float h = ItemEntity.getSpin(itemEntityRenderState.ageInTicks, itemEntityRenderState.bobOffset);
		poseStack.mulPose(Axis.YP.rotation(h));

		float tiltX = Mth.sin(itemEntityRenderState.bobOffset * 3.0F) * 0.35F;
		float tiltZ = Mth.cos(itemEntityRenderState.bobOffset * 2.0F) * 0.35F;
		poseStack.mulPose(Axis.XP.rotation(tiltX));
		poseStack.mulPose(Axis.ZP.rotation(tiltZ));

		ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, itemEntityRenderState.lightCoords, itemEntityRenderState, this.random, aabb);
		poseStack.popPose();
		super.submit(itemEntityRenderState, poseStack, submitNodeCollector, cameraRenderState);
		ci.cancel();
	}
}
