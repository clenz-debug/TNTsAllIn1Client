package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Own 3D skin layers: a skin's second layer (hat, jacket, sleeves, pants) drawn as real geometry with
 * depth instead of vanilla's flat sheets floating over the body. Replaces the bundled "3D Skin Layers"
 * mod, whose license allows neither redistribution nor commercial use - written from scratch, nothing
 * taken from that mod.
 *
 * <p>Per player and frame, {@link #prepare} decides which parts are drawn in 3D and switches vanilla's
 * flat version of exactly those off; {@link SkinLayers3dLayer} then draws them. Everything else keeps
 * vanilla's look: players further away than the chosen distance, invisible or glowing players (their
 * translucent/outline rendering goes through the model itself), and parts covered by armor, which sits
 * closer to the body than the 3D layer would.
 */
public final class SkinLayers3d {
	/**
	 * Low enough for a layer that barely stands off (own user request). Not lower: the parts differ
	 * by a few hundredths of a pixel in how far they stand off, and every one has to stay above zero.
	 */
	public static final int MIN_DEPTH_PERCENT = 10;
	public static final int MAX_DEPTH_PERCENT = 100;
	public static final int MIN_DISTANCE = 4;
	public static final int MAX_DISTANCE = 64;

	/** The original mod, if someone adds it themselves - both at once would draw every layer twice. */
	private static final boolean EXTERNAL_MOD_LOADED = FabricLoader.getInstance().isModLoaded("skinlayers3d");

	private SkinLayers3d() {
	}

	/** False while the original 3D Skin Layers mod is installed - it then does the job, and ours stays out of the menu. */
	public static boolean isAvailable() {
		return !EXTERNAL_MOD_LOADED;
	}

	private static boolean isActive() {
		return isAvailable() && ClientConfig.get().skinLayers3dEnabled;
	}

	/**
	 * Called once the render state is fully extracted. Stores which parts {@link SkinLayers3dLayer} draws
	 * and hides vanilla's flat version of them - {@code PlayerModel.setupAnim} is the only reader of the
	 * {@code show...} flags.
	 */
	public static void prepare(AvatarRenderState state, PlayerModel model) {
		SkinLayers3dStateAccess access = (SkinLayers3dStateAccess) state;
		access.tntsallin1client$setSkinLayers3dParts(0);
		if (!isActive() || state.skin == null || state.isInvisible || state.isSpectator || state.appearsGlowing()) {
			return;
		}
		ClientConfig config = ClientConfig.get();
		double maxDistance = Math.clamp(config.skinLayers3dDistance, MIN_DISTANCE, MAX_DISTANCE);
		if (state.distanceToCameraSq > maxDistance * maxDistance) {
			return;
		}

		int parts = 0;
		if (config.skinLayers3dHead && state.showHat && !HumanoidArmorLayer.shouldRender(state.headEquipment, EquipmentSlot.HEAD)) {
			parts |= SkinLayerPart.HAT.bit;
		}
		boolean chestArmor = HumanoidArmorLayer.shouldRender(state.chestEquipment, EquipmentSlot.CHEST);
		if (config.skinLayers3dJacket && state.showJacket && !chestArmor) {
			parts |= SkinLayerPart.JACKET.bit;
		}
		if (config.skinLayers3dSleeves && !chestArmor) {
			if (state.showLeftSleeve) {
				parts |= SkinLayerPart.LEFT_SLEEVE.bit;
			}
			if (state.showRightSleeve) {
				parts |= SkinLayerPart.RIGHT_SLEEVE.bit;
			}
		}
		// Boots sit further out than the layer, leggings closer to the leg - only those get in the way
		if (config.skinLayers3dPants && !HumanoidArmorLayer.shouldRender(state.legsEquipment, EquipmentSlot.LEGS)) {
			if (state.showLeftPants) {
				parts |= SkinLayerPart.LEFT_PANTS.bit;
			}
			if (state.showRightPants) {
				parts |= SkinLayerPart.RIGHT_PANTS.bit;
			}
		}
		if (parts == 0 || SkinLayerMeshes.get(state.skin.body().texturePath(), model) == null) {
			return;
		}

		state.showHat &= (parts & SkinLayerPart.HAT.bit) == 0;
		state.showJacket &= (parts & SkinLayerPart.JACKET.bit) == 0;
		state.showLeftSleeve &= (parts & SkinLayerPart.LEFT_SLEEVE.bit) == 0;
		state.showRightSleeve &= (parts & SkinLayerPart.RIGHT_SLEEVE.bit) == 0;
		state.showLeftPants &= (parts & SkinLayerPart.LEFT_PANTS.bit) == 0;
		state.showRightPants &= (parts & SkinLayerPart.RIGHT_PANTS.bit) == 0;
		access.tntsallin1client$setSkinLayers3dParts(parts);
	}

	/** Draws {@code part}; {@code pose} is the model's own space, the part's transform is applied here. */
	static void submitPart(PoseStack pose, SubmitNodeCollector collector, Identifier skin, PlayerModel model,
			SkinLayerPart part, SkinLayerMesh mesh, int light, int overlay) {
		if (mesh.isEmpty()) {
			return;
		}
		pose.pushPose();
		part.base(model).translateAndRotate(pose);
		part.overlay(model).translateAndRotate(pose);
		// Same render type as the player model itself, so half-transparent layer pixels blend the same way
		collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(skin),
				(lastPose, consumer) -> mesh.render(lastPose, consumer, light, overlay));
		pose.popPose();
	}

	/** Whether the first-person arm's sleeve is drawn in 3D for this skin - then vanilla's flat sleeve is left out. */
	public static boolean handSleeveIn3d(Identifier skin, PlayerModel model) {
		return isActive() && ClientConfig.get().skinLayers3dSleeves && SkinLayerMeshes.get(skin, model) != null;
	}

	/** The first-person arm's sleeve. {@code arm} is the model's left or right arm, already posed by vanilla. */
	public static void submitHandSleeve(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin,
			PlayerModel model, ModelPart arm) {
		SkinLayerMesh[] meshes = SkinLayerMeshes.get(skin, model);
		if (meshes == null) {
			return;
		}
		SkinLayerPart part = arm == model.leftArm ? SkinLayerPart.LEFT_SLEEVE : SkinLayerPart.RIGHT_SLEEVE;
		submitPart(pose, collector, skin, model, part, meshes[part.ordinal()], light, OverlayTexture.NO_OVERLAY);
	}
}
