package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import com.tntsallin1client.skinlayers.SkinLayers3dFeature;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam: keeps the player's own body visible while the camera is elsewhere. The renderer draws
 * the player playing on this client only if the camera is on that very player (the check right at
 * the start of its `render`, bytecode-checked) - so with the camera on the freecam the body would
 * vanish. While freecam is active the player is treated like any other player here.
 *
 * <p>Own 3D skin layers, see {@link SkinLayers3d}: adds the layer that draws them, decides per
 * player which parts it draws, and gives the first-person arm its 3D sleeve.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	protected PlayerRendererMixin(EntityRenderDispatcher dispatcher, PlayerModel<AbstractClientPlayer> model, float shadowSize) {
		super(dispatcher, model, shadowSize);
	}

	@Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;Z)V", at = @At("RETURN"))
	private void tntsallin1client$addSkinLayers3d(EntityRenderDispatcher dispatcher, boolean slim, CallbackInfo ci) {
		this.addLayer(new SkinLayers3dFeature(this));
	}

	/** The game has just set which parts of the model show for this player - before it draws the body, and before it draws the first-person arm. */
	@Inject(method = "setModelProperties", at = @At("RETURN"))
	private void tntsallin1client$prepareSkinLayers3d(AbstractClientPlayer player, CallbackInfo ci) {
		SkinLayers3d.prepare(player, this.getModel());
	}

	@Inject(method = "renderRightHand", at = @At("RETURN"))
	private void tntsallin1client$renderRightHandSleeve(AbstractClientPlayer player, CallbackInfo ci) {
		SkinLayers3d.renderHandSleeve(player, this.getModel(), true);
	}

	@Inject(method = "renderLeftHand", at = @At("RETURN"))
	private void tntsallin1client$renderLeftHandSleeve(AbstractClientPlayer player, CallbackInfo ci) {
		SkinLayers3d.renderHandSleeve(player, this.getModel(), false);
	}

	@Redirect(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isLocalPlayer()Z"))
	private boolean tntsallin1client$showOwnBodyWhileFreecam(AbstractClientPlayer player) {
		return player.isLocalPlayer() && !FreecamHandler.isActive();
	}
}
