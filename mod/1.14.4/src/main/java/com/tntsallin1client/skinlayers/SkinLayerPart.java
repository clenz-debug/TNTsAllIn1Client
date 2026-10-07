package com.tntsallin1client.skinlayers;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * The six overlay parts of a player skin ("second layer"). In this version's player model each one
 * is a part of its own that the model keeps at the position and angle of the body part it covers.
 */
enum SkinLayerPart {
	HAT(0.02F),
	JACKET(0.0F),
	LEFT_SLEEVE(-0.02F),
	RIGHT_SLEEVE(-0.02F),
	LEFT_PANTS(-0.04F),
	RIGHT_PANTS(-0.06F);

	/**
	 * Tiny per-part difference in how far the layer stands off the body, in model pixels. Neighbouring
	 * parts overlap a little (jacket and pants at the waist, the two pants legs in the middle), and
	 * with the exact same distance their surfaces would lie in one plane and flicker.
	 */
	final float depthOffset;
	final int bit;

	SkinLayerPart(float depthOffset) {
		this.depthOffset = depthOffset;
		this.bit = 1 << ordinal();
	}

	/** The overlay part itself - its box tells where the layer sits and which part of the skin it shows. */
	ModelPart overlay(PlayerModel<AbstractClientPlayer> model) {
		switch (this) {
			case HAT:
				return model.hat;
			case JACKET:
				return model.jacket;
			case LEFT_SLEEVE:
				return model.leftSleeve;
			case RIGHT_SLEEVE:
				return model.rightSleeve;
			case LEFT_PANTS:
				return model.leftPants;
			default:
				return model.rightPants;
		}
	}

	/**
	 * The body part the overlay covers. Its position is the overlay's too, and it is the one to take
	 * it from: a part that is switched to invisible - as the overlay is while we draw it in 3D -
	 * does not apply its position.
	 */
	ModelPart base(PlayerModel<AbstractClientPlayer> model) {
		switch (this) {
			case HAT:
				return model.head;
			case JACKET:
				return model.body;
			case LEFT_SLEEVE:
				return model.leftArm;
			case RIGHT_SLEEVE:
				return model.rightArm;
			case LEFT_PANTS:
				return model.leftLeg;
			default:
				return model.rightLeg;
		}
	}
}
