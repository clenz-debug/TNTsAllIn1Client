package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import com.tntsallin1client.skinlayers.SkinLayers3dStateAccess;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Own 3D skin layers on the first-person arm. Since 26.3 {@code renderPlayerHand} takes the "draw
 * the sleeve" flag from the player's {@code AvatarRenderState} ({@code showLeftSleeve}/
 * {@code showRightSleeve}, checked with javap) - the very flags {@link SkinLayers3d#prepare}
 * switches off for the parts it draws in 3D. So the arm arrived at {@code AvatarRenderer#renderHand}
 * with "no sleeve" and got neither the flat nor the 3D one (own user report). This passes on which
 * sleeves the 3D layers took over, for {@link AvatarRendererMixin} to draw them.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonSleeveMixin {
	@Inject(method = "renderPlayerHand", at = @At("HEAD"))
	private void tntsallin1client$rememberSleeves(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidArm arm,
			PlayerRenderState player, CallbackInfo ci) {
		SkinLayers3d.setFirstPersonParts(player.avatarRenderState instanceof SkinLayers3dStateAccess access
				? access.tntsallin1client$getSkinLayers3dParts() : 0);
	}

	@Inject(method = "renderPlayerHand", at = @At("RETURN"))
	private void tntsallin1client$forgetSleeves(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidArm arm,
			PlayerRenderState player, CallbackInfo ci) {
		SkinLayers3d.setFirstPersonParts(0);
	}
}
