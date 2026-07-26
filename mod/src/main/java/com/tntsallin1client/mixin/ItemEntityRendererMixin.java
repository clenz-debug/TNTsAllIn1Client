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
 * Phase 5o: purely cosmetic "item physics" for dropped items. Deliberately
 * does NOT attempt real physics (items rolling to a stop, sliding, not
 * clipping through blocks): that needs the actual entity position/orientation
 * to be correct, which the server simulates and syncs - a client-only mod can
 * only affect rendering, not real position, so it would either look wrong
 * against the server's authoritative state or require the server to run the
 * same mod too. That's exactly the trade-off in the well-known "ItemPhysic"
 * mod (LGPL-2.1, checked on Modrinth): Modrinth lists it as server_side:
 * required, not just client. Bundling that would silently stop doing
 * anything real the moment the user joins a server that doesn't also run it
 * - i.e. almost every server, including this project's own explicit "works
 * on any vanilla-compatible server, like Lunar/Badlion" premise.
 *
 * <p>What IS honestly achievable client-only, and what this does: once an
 * item is resting on solid ground (not falling, not in water/lava), stop the
 * vanilla spin and lay it flat instead of upright, at a fixed per-item angle
 * derived from {@code bobOffset} (stable - it never changes over time, so
 * "not rotating" actually means that, not just "rotating slower"). While
 * airborne or in a fluid it falls back to plain vanilla rendering (bob +
 * spin) instead - vanilla's own fluid buoyancy already makes it float/bob at
 * the surface correctly, and forcing the flat "resting" pose while it's
 * still moving would look wrong.
 *
 * <p>{@code onGround()}/{@code isInWater()} live on the entity, not on
 * {@code ItemEntityRenderState} - and {@code submit(...)} only ever receives
 * the render state, never the live entity (the point of the render-state
 * split is to not need it there). So a second mixin pair
 * ({@link ItemEntityRenderStateMixin}/{@link ItemPhysicsStateAccess}) adds
 * two fields to the render state and populates them here in
 * {@code extractRenderState(...)}, exactly like vanilla itself does for
 * {@code bobOffset}.
 *
 * <p>No clean single-call extension point to add one extra rotation to an
 * existing PoseStack transform sequence without a hand-written bytecode
 * INVOKE target string (wrong by even one character and the mixin fails to
 * apply at game startup, not just "doesn't work" - unverifiable without
 * actually launching the game). Cancels the whole method instead and
 * reimplements it, same trade-off already accepted for 5l's hitbox color -
 * but only when the resting pose actually applies; otherwise this doesn't
 * cancel at all and vanilla's own {@code submit} runs unmodified.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
	@Shadow
	@Final
	private RandomSource random;

	protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tntsallin1client$onExtractRenderState(
			ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float partialTick, CallbackInfo ci) {
		ItemPhysicsStateAccess access = (ItemPhysicsStateAccess) itemEntityRenderState;
		access.tntsallin1client$setOnGround(itemEntity.onGround());
		access.tntsallin1client$setInWater(itemEntity.isInWater());
	}

	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onSubmit(
			ItemEntityRenderState itemEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraRenderState, CallbackInfo ci) {
		if (!ClientConfig.get().itemTiltEnabled || itemEntityRenderState.item.isEmpty()) {
			return;
		}

		ItemPhysicsStateAccess access = (ItemPhysicsStateAccess) itemEntityRenderState;
		if (!access.tntsallin1client$isOnGround() || access.tntsallin1client$isInWater()) {
			// Falling or floating - leave vanilla's own bob/spin (and its correct fluid
			// buoyancy) alone instead of forcing the flat resting pose onto a moving item.
			return;
		}

		poseStack.pushPose();
		AABB aabb = itemEntityRenderState.item.getModelBoundingBox();
		float f = -((float) aabb.minY) + 0.0625F;
		poseStack.translate(0.0F, f, 0.0F);

		// Fixed per-item yaw so a pile of items doesn't all lie at the exact same angle -
		// derived from bobOffset alone (not ageInTicks), so unlike vanilla's spin it never
		// changes frame to frame.
		float restYaw = itemEntityRenderState.bobOffset * ((float) Math.PI * 2.0F);
		poseStack.mulPose(Axis.YP.rotation(restYaw));
		// Lay the normally-upright item flat, face up, instead of standing on its edge.
		poseStack.mulPose(Axis.XP.rotation((float) (Math.PI / 2.0)));

		ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, itemEntityRenderState.lightCoords, itemEntityRenderState, this.random, aabb);
		poseStack.popPose();
		super.submit(itemEntityRenderState, poseStack, submitNodeCollector, cameraRenderState);
		ci.cancel();
	}
}
