package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import com.tntsallin1client.skinlayers.SkinLayers3dFeature;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam: keeps the player's own body visible while the camera is elsewhere. The renderer draws
 * the player playing on this client only if the camera is on that very player (the check right at
 * the start of its `render`) - so with the camera on the freecam the body would vanish. While
 * freecam is active the player is treated like any other player here.
 *
 * <p>Own 3D skin layers, see {@link SkinLayers3d}: adds the feature that draws them, decides per
 * player which parts it draws, and gives the first-person arm its 3D sleeve.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity> {
	protected PlayerEntityRendererMixin(EntityRenderDispatcher dispatcher, EntityModel model, float shadowSize) {
		super(dispatcher, model, shadowSize);
	}

	@Shadow
	public abstract PlayerEntityModel getModel();

	@Redirect(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isMainPlayer()Z"))
	private boolean tnt$showOwnBodyWhileFreecam(AbstractClientPlayerEntity player) {
		return player.isMainPlayer() && !FreecamHandler.isActive();
	}

	@Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRenderDispatcher;Z)V", at = @At("RETURN"))
	private void tnt$addSkinLayers3d(EntityRenderDispatcher dispatcher, boolean slim, CallbackInfo ci) {
		this.addFeature(new SkinLayers3dFeature((PlayerEntityRenderer) (Object) this));
	}

	/** The game has just set which parts of the model show for this player - before it draws the body, and before it draws the first-person arm. */
	@Inject(method = "setModelPose(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("RETURN"))
	private void tnt$prepareSkinLayers3d(AbstractClientPlayerEntity player, CallbackInfo ci) {
		SkinLayers3d.prepare(player, this.getModel());
	}

	@Inject(method = "renderRightArm(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("RETURN"))
	private void tnt$renderRightHandSleeve(AbstractClientPlayerEntity player, CallbackInfo ci) {
		SkinLayers3d.renderHandSleeve(player, this.getModel(), true);
	}

	@Inject(method = "renderLeftArm(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("RETURN"))
	private void tnt$renderLeftHandSleeve(AbstractClientPlayerEntity player, CallbackInfo ci) {
		SkinLayers3d.renderHandSleeve(player, this.getModel(), false);
	}
}
