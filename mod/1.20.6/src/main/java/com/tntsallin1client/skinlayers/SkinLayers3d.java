package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;

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

	private static final boolean EXTERNAL_MOD_LOADED = FabricLoader.getInstance().isModLoaded("skinlayers3d");

	/**
	 * The parts picked for the player whose model was set up last. The renderer sets up the model and
	 * runs its layers back to back for one player at a time, so the layer finds its own player's here.
	 */
	private static int preparedParts;

	private SkinLayers3d() {
	}

	/** Not while the "3D Skin Layers" mod itself is installed - it would draw the same parts a second time. */
	public static boolean isAvailable() {
		return !EXTERNAL_MOD_LOADED;
	}

	private static boolean isActive() {
		return isAvailable() && ClientConfig.get().skinLayers3dEnabled;
	}

	static int preparedParts() {
		return preparedParts;
	}

	/**
	 * Called right after the renderer decided which model parts are visible for {@code player}.
	 *
	 * @param distanceSq the player's squared distance to the camera
	 */
	public static void prepare(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, double distanceSq) {
		preparedParts = 0;
		if (!isActive() || player.isInvisible() || player.isSpectator() || Minecraft.getInstance().shouldEntityAppearGlowing(player)) {
			return;
		}
		ClientConfig config = ClientConfig.get();
		double maxDistance = Math.clamp(config.skinLayers3dDistance, MIN_DISTANCE, MAX_DISTANCE);
		if (distanceSq > maxDistance * maxDistance) {
			return;
		}

		int parts = 0;
		if (config.skinLayers3dHead && model.hat.visible && !wearsArmor(player, EquipmentSlot.HEAD)) {
			parts |= SkinLayerPart.HAT.bit;
		}
		boolean chestArmor = wearsArmor(player, EquipmentSlot.CHEST);
		if (config.skinLayers3dJacket && model.jacket.visible && !chestArmor) {
			parts |= SkinLayerPart.JACKET.bit;
		}
		if (config.skinLayers3dSleeves && !chestArmor) {
			if (model.leftSleeve.visible) {
				parts |= SkinLayerPart.LEFT_SLEEVE.bit;
			}
			if (model.rightSleeve.visible) {
				parts |= SkinLayerPart.RIGHT_SLEEVE.bit;
			}
		}
		if (config.skinLayers3dPants && !wearsArmor(player, EquipmentSlot.LEGS)) {
			if (model.leftPants.visible) {
				parts |= SkinLayerPart.LEFT_PANTS.bit;
			}
			if (model.rightPants.visible) {
				parts |= SkinLayerPart.RIGHT_PANTS.bit;
			}
		}
		if (parts == 0 || SkinLayerMeshes.get(player.getSkin().texture(), model) == null) {
			return;
		}

		for (SkinLayerPart part : SkinLayerPart.values()) {
			if ((parts & part.bit) != 0) {
				part.overlay(model).visible = false;
			}
		}
		preparedParts = parts;
	}

	private static boolean wearsArmor(AbstractClientPlayer player, EquipmentSlot slot) {
		return player.getItemBySlot(slot).getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot;
	}

	/**
	 * One part at the place its flat version would be drawn. The overlay parts are not children of the
	 * body parts in this version - the model copies the body part's pose onto them.
	 */
	static void renderPart(PoseStack pose, MultiBufferSource buffer, ResourceLocation skin, PlayerModel<?> model,
			SkinLayerPart part, SkinLayerMesh mesh, int light, int overlay) {
		if (mesh.isEmpty()) {
			return;
		}
		pose.pushPose();
		part.overlay(model).translateAndRotate(pose);
		mesh.render(pose.last(), buffer.getBuffer(RenderType.entityTranslucent(skin)), light, overlay);
		pose.popPose();
	}

	/** Whether the first-person hand's sleeve is drawn in 3D - then the flat one must not be. */
	public static boolean handSleeveIn3d(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, ModelPart sleeve) {
		PlayerModelPart shown = sleeve == model.leftSleeve ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE;
		return isActive() && ClientConfig.get().skinLayers3dSleeves && player.isModelPartShown(shown)
				&& SkinLayerMeshes.get(player.getSkin().texture(), model) != null;
	}

	/** The first-person hand's sleeve - the pose is where the game just drew the bare arm. */
	public static void renderHandSleeve(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
			PlayerModel<AbstractClientPlayer> model, ModelPart sleeve) {
		ResourceLocation skin = player.getSkin().texture();
		SkinLayerMesh[] meshes = SkinLayerMeshes.get(skin, model);
		if (meshes == null) {
			return;
		}
		SkinLayerPart part = sleeve == model.leftSleeve ? SkinLayerPart.LEFT_SLEEVE : SkinLayerPart.RIGHT_SLEEVE;
		renderPart(pose, buffer, skin, model, part, meshes[part.ordinal()], light, OverlayTexture.NO_OVERLAY);
	}
}
