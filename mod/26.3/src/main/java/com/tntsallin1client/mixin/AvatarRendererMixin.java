package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import com.tntsallin1client.skinlayers.SkinLayers3dLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Own 3D skin layers, see {@link SkinLayers3d}: adds the layer that draws them, decides per player
 * which parts it draws, and gives the first-person arm its 3D sleeve. Also keeps the player's cape
 * still while freecam has frozen them, see {@link #tntsallin1client$steadyFlightDataWhileFrozen}.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity>
		extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
	/** Set by {@link #tntsallin1client$hideFlatSleeve} for the {@code renderHand} call it belongs to. */
	@Unique
	private boolean handSleeveIn3d;

	protected AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void tntsallin1client$addSkinLayers3d(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		this.addLayer(new SkinLayers3dLayer(this));
	}

	// Explicit descriptor: the bridge methods taking LivingEntity/Entity share the name. At the tail
	// everything the decision needs (distance, invisibility, outline, equipment) is already extracted.
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void tntsallin1client$prepareSkinLayers3d(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		SkinLayers3d.prepare(state, this.getModel());
	}

	/**
	 * Freecam: {@code extractFlightData} stores "fall flying ticks + partial tick", and the cape's
	 * lean is then scaled by {@code 1 - (that)^2 / 100} ({@code AvatarRenderState#fallFlyingScale},
	 * read in {@code extractCapeState}, bytecode-verified). For a player not gliding that is up to
	 * 1 % less lean within every tick, jumping back at the next one - unnoticeable on a moving
	 * player, a slight twitch on the frozen one. The same partial tick blends the previous and
	 * current velocity for a gliding player's body roll. A fixed one keeps both still.
	 */
	@ModifyArg(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;extractFlightData(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V"),
			index = 2)
	private float tntsallin1client$steadyFlightDataWhileFrozen(Avatar entity, AvatarRenderState state, float partialTick) {
		return FreecamHandler.isActive() && entity == Minecraft.getInstance().player ? 0.0F : partialTick;
	}

	/**
	 * {@code renderHand}'s last parameter says whether the flat sleeve is drawn over the first-person
	 * arm. Since 26.3 it is already false for a sleeve the 3D layers took over (see
	 * {@link FirstPersonSleeveMixin}), so "wanted" is either the parameter or that take-over.
	 */
	@ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true)
	private boolean tntsallin1client$hideFlatSleeve(boolean sleeve, PoseStack pose, SubmitNodeCollector collector, int light,
			Identifier skin, ModelPart arm, boolean sleeveArgument) {
		PlayerModel model = this.getModel();
		boolean wanted = sleeve || SkinLayers3d.firstPersonSleeveTakenOver(arm == model.leftArm);
		this.handSleeveIn3d = wanted && SkinLayers3d.handSleeveIn3d(skin, model);
		return sleeve && !this.handSleeveIn3d;
	}

	@Inject(method = "renderHand", at = @At("TAIL"))
	private void tntsallin1client$submitHandSleeve(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin,
			ModelPart arm, boolean sleeve, CallbackInfo ci) {
		if (this.handSleeveIn3d) {
			SkinLayers3d.submitHandSleeve(pose, collector, light, skin, this.getModel(), arm);
		}
	}
}
