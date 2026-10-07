package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Own 3D skin layers: a skin's second layer (hat, jacket, sleeves, pants) drawn as real geometry
 * with depth instead of the game's flat sheets floating over the body. Same feature, settings and
 * construction as in the other versions.
 *
 * <p>Per player and frame, {@link #prepare} decides which parts are drawn in 3D and switches the
 * game's flat version of exactly those off; {@link SkinLayers3dFeature} then draws them, and
 * {@link #renderHandSleeve} the one on the first-person arm. Everything else keeps the game's look:
 * players further away than the chosen distance, invisible players and spectators (their
 * see-through look goes through the model itself), and parts covered by armor, which sits closer to
 * the body than the 3D layer would.
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

	// The armor slots of a player's inventory.
	private static final int LEGS_SLOT = 1;
	private static final int CHEST_SLOT = 2;
	private static final int HEAD_SLOT = 3;
	/** The size models are drawn at: one model pixel is a sixteenth of a block. */
	private static final float MODEL_SCALE = 0.0625F;

	private static final SkinLayerPart[] PARTS = SkinLayerPart.values();

	/** The player {@link #prepare} was last called for, and what it decided - read back when that player's layers are drawn right after. */
	private static AbstractClientPlayer preparedPlayer;
	private static int preparedParts;

	private SkinLayers3d() {
	}

	/**
	 * Called right after the game has decided which parts of the model it shows for this player.
	 * Notes which parts get drawn in 3D and hides the game's flat version of them.
	 */
	public static void prepare(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
		preparedPlayer = player;
		preparedParts = 0;
		ClientConfig config = ClientConfig.get();
		if (!config.skinLayers3dEnabled || player.isInvisible() || player.isSpectator()) {
			return;
		}
		Entity camera = Minecraft.getInstance().getCameraEntity();
		double maxDistance = Mth.clamp(config.skinLayers3dDistance, MIN_DISTANCE, MAX_DISTANCE);
		if (camera != null && player.distanceToSqr(camera) > maxDistance * maxDistance) {
			return;
		}

		int parts = 0;
		if (config.skinLayers3dHead && model.hat.visible && !wearsArmor(player, HEAD_SLOT)) {
			parts |= SkinLayerPart.HAT.bit;
		}
		boolean chestArmor = wearsArmor(player, CHEST_SLOT);
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
		// Boots sit further out than the layer, leggings closer to the leg - only those get in the way.
		if (config.skinLayers3dPants && !wearsArmor(player, LEGS_SLOT)) {
			if (model.leftPants.visible) {
				parts |= SkinLayerPart.LEFT_PANTS.bit;
			}
			if (model.rightPants.visible) {
				parts |= SkinLayerPart.RIGHT_PANTS.bit;
			}
		}
		if (parts == 0 || SkinLayerMeshes.get(skinOf(player), model) == null) {
			return;
		}

		for (SkinLayerPart part : PARTS) {
			if ((parts & part.bit) != 0) {
				part.overlay(model).visible = false;
			}
		}
		preparedParts = parts;
	}

	/** A pumpkin or a head in the helmet slot (or an elytra on the chest) is not armor and leaves the hat layer alone, like in the other versions. */
	private static boolean wearsArmor(AbstractClientPlayer player, int slot) {
		ItemStack stack = player.inventory.armor.get(slot);
		return stack.getItem() instanceof ArmorItem;
	}

	private static ResourceLocation skinOf(AbstractClientPlayer player) {
		return player.getSkinTextureLocation();
	}

	/** Draws the parts picked for this player. The model is posed; called in the model's own space. */
	static void renderLayers(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, float scale) {
		if (player != preparedPlayer || preparedParts == 0) {
			return;
		}
		SkinLayerMesh[] meshes = bindAndGet(player, model);
		if (meshes == null) {
			return;
		}
		GlStateManager.pushMatrix();
		// The model draws itself this much lower while sneaking.
		if (model.sneaking) {
			GlStateManager.translatef(0.0F, 0.2F, 0.0F);
		}
		for (SkinLayerPart part : PARTS) {
			if ((preparedParts & part.bit) != 0) {
				renderPart(model, part, meshes, scale);
			}
		}
		GlStateManager.popMatrix();
	}

	/**
	 * The first-person arm's sleeve, right after the game has drawn the arm (its own flat sleeve was
	 * switched off by {@link #prepare}, which the game calls for the arm as well).
	 */
	public static void renderHandSleeve(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, boolean rightArm) {
		SkinLayerPart part = rightArm ? SkinLayerPart.RIGHT_SLEEVE : SkinLayerPart.LEFT_SLEEVE;
		if (player != preparedPlayer || (preparedParts & part.bit) == 0) {
			return;
		}
		SkinLayerMesh[] meshes = bindAndGet(player, model);
		if (meshes != null) {
			renderPart(model, part, meshes, MODEL_SCALE);
		}
	}

	private static SkinLayerMesh[] bindAndGet(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
		ResourceLocation skin = skinOf(player);
		SkinLayerMesh[] meshes = SkinLayerMeshes.get(skin, model);
		// Other layers drawn before this one leave their own texture bound.
		Minecraft.getInstance().getTextureManager().bind(skin);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		return meshes;
	}

	private static void renderPart(PlayerModel<AbstractClientPlayer> model, SkinLayerPart part, SkinLayerMesh[] meshes, float scale) {
		GlStateManager.pushMatrix();
		part.base(model).translateTo(scale);
		meshes[part.ordinal()].render();
		GlStateManager.popMatrix();
	}
}
