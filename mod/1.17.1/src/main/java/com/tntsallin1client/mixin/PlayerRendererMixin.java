package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import com.tntsallin1client.skinlayers.SkinLayers3dLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D skin layers, see {@link SkinLayers3d}: adds the layer to both player renderers (wide and slim),
 * lets {@code prepare} pick the parts right after the game decided which model parts are visible, and
 * draws the first-person hand's sleeve.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	@Unique
	private boolean handSleeveIn3d;

	protected PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void tntsallin1client$addSkinLayers3d(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		this.addLayer(new SkinLayers3dLayer(this));
	}

	@Inject(method = "setModelProperties", at = @At("TAIL"))
	private void tntsallin1client$prepareSkinLayers3d(AbstractClientPlayer player, CallbackInfo ci) {
		SkinLayers3d.prepare(player, this.getModel(), this.entityRenderDispatcher.distanceToSqr(player));
	}

	/**
	 * The hand: {@code renderHand} sets the model up for a bare arm and then draws arm and sleeve
	 * (bytecode-read). Once that set-up is done the flat sleeve is switched off if ours replaces it -
	 * whatever {@code prepare} picked for the body does not count here, the hand is never under armor.
	 */
	@Inject(method = "renderHand", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/model/PlayerModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"))
	private void tntsallin1client$hideFlatSleeve(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
			ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
		this.handSleeveIn3d = SkinLayers3d.handSleeveIn3d(player, this.getModel(), sleeve);
		if (this.handSleeveIn3d) {
			sleeve.visible = false;
		}
	}

	@Inject(method = "renderHand", at = @At("TAIL"))
	private void tntsallin1client$renderHandSleeve(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
			ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
		if (this.handSleeveIn3d) {
			SkinLayers3d.renderHandSleeve(pose, buffer, light, player, this.getModel(), sleeve);
		}
	}
}
