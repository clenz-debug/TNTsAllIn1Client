package com.tntsallin1client.itemphysics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Purely cosmetic "item physics" for dropped items, as in the Fabric versions: an item no longer
 * spins or bobs - it keeps one fixed angle of its own, and once it rests on the ground it lies
 * flat. Only in water it looks as the game draws it. Where the item really is stays the server's
 * business; this changes nothing but how it is drawn.
 *
 * <p>The game positions a dropped item with one move (to the item's place, lifted by the bobbing)
 * and one turn (the spin). `ItemEntityRendererMixin` hands both over to here.
 */
public final class ItemTilt {
	private static final float DEGREES_PER_RADIAN = 57.295776F;
	/** How high a flat-lying item's middle is above the ground: half its thickness and a little air. */
	private static final float FLAT_LIFT = 0.03F;
	/** Half of what the game moves each further copy of a stack apart - they pile up when lying flat. */
	private static final float FLAT_COPY_HALF_STEP = 0.0234375F;

	/** What the game is positioning right now - set at the start of its method, on the render thread. */
	private static ItemEntity entity;
	private static double baseY;
	private static float tickDelta;
	private static boolean hasDepth;

	private ItemTilt() {
	}

	public static void begin(ItemEntity itemEntity, double y, float delta, BakedModel model) {
		entity = itemEntity;
		baseY = y;
		tickDelta = delta;
		hasDepth = model.hasDepth();
	}

	private static boolean applies() {
		return ClientConfig.get().itemTiltEnabled && entity != null && !entity.isTouchingWater();
	}

	/** In place of the game's move to the item's place. `y` already has the bobbing and the model's own lift in it. */
	public static void translate(float x, float y, float z) {
		if (!applies()) {
			GlStateManager.translate(x, y, z);
			return;
		}
		if (entity.onGround && !hasDepth) {
			// A flat picture lying down: its middle just above the ground, a bigger stack's pile resting on it.
			GlStateManager.translate(x, (float) baseY + FLAT_LIFT + FLAT_COPY_HALF_STEP * (copies(entity.getItemStack()) - 1), z);
			return;
		}
		// No bobbing; a block-shaped item keeps the lift that puts its underside on the ground.
		float bobbing = MathHelper.sin((entity.getAge() + tickDelta) / 10.0F + entity.hoverHeight) * 0.1F + 0.1F;
		GlStateManager.translate(x, y - bobbing, z);
	}

	/** In place of the game's spin. */
	public static void rotate(float angle, float x, float y, float z) {
		if (!applies()) {
			GlStateManager.rotate(angle, x, y, z);
			return;
		}
		// The value the game starts every item's spin and bobbing from is random per item and never
		// changes - a pile of items doesn't all lie at the same angle.
		GlStateManager.rotate(entity.hoverHeight * DEGREES_PER_RADIAN, 0.0F, 1.0F, 0.0F);
		if (entity.onGround) {
			GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
		}
	}

	/** How many copies the game draws for a stack (bytecode-read from the renderer). */
	private static int copies(ItemStack stack) {
		if (stack == null) {
			return 1;
		}
		if (stack.count > 48) {
			return 5;
		}
		if (stack.count > 32) {
			return 4;
		}
		if (stack.count > 16) {
			return 3;
		}
		return stack.count > 1 ? 2 : 1;
	}
}
