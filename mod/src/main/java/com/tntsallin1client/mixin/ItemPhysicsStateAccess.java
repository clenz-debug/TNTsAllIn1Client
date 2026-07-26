package com.tntsallin1client.mixin;

/**
 * Duck interface implemented by {@link ItemEntityRenderStateMixin} so
 * {@link ItemEntityRendererMixin} can read/write the ground/water flags it
 * adds to {@code ItemEntityRenderState} - that render-state class has no
 * such fields itself, and {@code submit(...)} only ever gets the render
 * state, never the live {@code ItemEntity} (the whole point of the
 * render-state split is to not need the live entity at submit time), so
 * this has to be captured once in {@code extractRenderState(...)} and
 * carried across on the state object, the same way vanilla itself does it
 * for e.g. {@code bobOffset}.
 */
public interface ItemPhysicsStateAccess {
	boolean tntsallin1client$isOnGround();

	void tntsallin1client$setOnGround(boolean onGround);

	boolean tntsallin1client$isInWater();

	void tntsallin1client$setInWater(boolean inWater);
}
