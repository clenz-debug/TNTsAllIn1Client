package com.tntsallin1client.skinlayers;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;

/**
 * The six overlay parts of a player skin ("second layer"). Each one is a child of the body part it
 * covers, so it moves with that part.
 */
public enum SkinLayerPart {
	HAT(0.02f),
	JACKET(0.0f),
	LEFT_SLEEVE(-0.02f),
	RIGHT_SLEEVE(-0.02f),
	LEFT_PANTS(-0.04f),
	RIGHT_PANTS(-0.06f);

	/**
	 * Tiny per-part difference in how far the layer stands off the body, in model pixels. Neighbouring
	 * parts overlap a little (jacket and pants at the waist, the two pants legs in the middle), and with
	 * the exact same distance their surfaces would lie in one plane and flicker.
	 */
	public final float depthOffset;
	public final int bit;

	SkinLayerPart(float depthOffset) {
		this.depthOffset = depthOffset;
		this.bit = 1 << this.ordinal();
	}

	/** The overlay part itself - its cube tells where the layer sits and which part of the skin it shows. */
	public ModelPart overlay(PlayerModel model) {
		return switch (this) {
			case HAT -> model.hat;
			case JACKET -> model.jacket;
			case LEFT_SLEEVE -> model.leftSleeve;
			case RIGHT_SLEEVE -> model.rightSleeve;
			case LEFT_PANTS -> model.leftPants;
			case RIGHT_PANTS -> model.rightPants;
		};
	}

	/** The body part the overlay is attached to. */
	public ModelPart base(PlayerModel model) {
		return switch (this) {
			case HAT -> model.head;
			case JACKET -> model.body;
			case LEFT_SLEEVE -> model.leftArm;
			case RIGHT_SLEEVE -> model.rightArm;
			case LEFT_PANTS -> model.leftLeg;
			case RIGHT_PANTS -> model.rightLeg;
		};
	}
}
