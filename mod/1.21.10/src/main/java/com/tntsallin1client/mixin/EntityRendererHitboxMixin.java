package com.tntsallin1client.mixin;

import com.google.common.collect.ImmutableList;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase 5l, extended 5aa/5ab: recolors the F3+B hitbox outline. In 1.21.10 the renderer collects an
 * entity's boxes into a {@link HitboxesRenderState} first, each with the color the game hardcodes for
 * it - the hitbox itself white, the vehicle mount marker yellow, the eye-height line red
 * ({@code LivingEntityRenderer}) and the ender dragon's sub-parts green ({@code EnderDragonRenderer}).
 * This swaps those colors for ours on the way out, and leaves a box out whose switch is off
 * ({@link ClientConfig}'s {@code customHitboxShow*}/{@code customHitbox*Color} fields). Boxes in any
 * other color (another mod's) pass through untouched.
 *
 * <p>{@code green} is the rare dev-only "compare against the local server entity" overlay, left
 * untouched. The view-direction arrow is not a box - see {@link HitboxFeatureRendererMixin}.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererHitboxMixin {
	@Inject(method = "extractHitboxes(Lnet/minecraft/world/entity/Entity;FZ)Lnet/minecraft/client/renderer/entity/state/HitboxesRenderState;",
			at = @At("RETURN"), cancellable = true)
	private void tntsallin1client$recolorHitboxes(Entity entity, float partialTick, boolean green,
			CallbackInfoReturnable<HitboxesRenderState> cir) {
		ClientConfig config = ClientConfig.get();
		if (green || !config.customHitboxColorEnabled) {
			return;
		}

		HitboxesRenderState state = cir.getReturnValue();
		ImmutableList.Builder<HitboxRenderState> boxes = ImmutableList.builder();
		for (HitboxRenderState box : state.hitboxes()) {
			if (tntsallin1client$hasColor(box, 1.0F, 1.0F, 1.0F)) {
				boxes.add(tntsallin1client$recolor(box, config.customHitboxColor));
			} else if (tntsallin1client$hasColor(box, 1.0F, 1.0F, 0.0F)) {
				if (config.customHitboxShowVehicleMarker) {
					boxes.add(tntsallin1client$recolor(box, config.customHitboxVehicleMarkerColor));
				}
			} else if (tntsallin1client$hasColor(box, 1.0F, 0.0F, 0.0F)) {
				if (config.customHitboxShowEyeHeight) {
					boxes.add(tntsallin1client$recolor(box, config.customHitboxEyeHeightColor));
				}
			} else if (tntsallin1client$hasColor(box, 0.25F, 1.0F, 0.0F)) {
				if (config.customHitboxShowDragonParts) {
					boxes.add(tntsallin1client$recolor(box, config.customHitboxDragonPartsColor));
				}
			} else {
				boxes.add(box);
			}
		}
		cir.setReturnValue(new HitboxesRenderState(state.viewX(), state.viewY(), state.viewZ(), boxes.build()));
	}

	@Unique
	private static boolean tntsallin1client$hasColor(HitboxRenderState box, float red, float green, float blue) {
		return box.red() == red && box.green() == green && box.blue() == blue;
	}

	@Unique
	private static HitboxRenderState tntsallin1client$recolor(HitboxRenderState box, int color) {
		return new HitboxRenderState(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1(),
				box.offsetX(), box.offsetY(), box.offsetZ(),
				ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color));
	}
}
