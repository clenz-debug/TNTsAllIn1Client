package com.tntsallin1client.itemphysics;

/**
 * Duck interface implemented by {@code ItemEntityRenderStateMixin} so
 * {@code ItemEntityRendererMixin} can read/write the ground/water flags it
 * adds to {@code ItemEntityRenderState} - that render-state class has no
 * such fields itself, and {@code submit(...)} only ever gets the render
 * state, never the live {@code ItemEntity} (the whole point of the
 * render-state split is to not need the live entity at submit time), so
 * this has to be captured once in {@code extractRenderState(...)} and
 * carried across on the state object, the same way vanilla itself does it
 * for e.g. {@code bobOffset}.
 *
 * <p>Deliberately lives outside {@code com.tntsallin1client.mixin}: that
 * whole package is declared as mixin-only in {@code tntsallin1client.mixins.json},
 * and Mixin refuses to load any plain (non-{@code @Mixin}) class from a
 * package it owns if something tries to reference it directly - which this
 * cast-to-interface pattern does, by design. Learned the hard way: crashed
 * with {@code IllegalClassLoadError} at startup the one time this sat in
 * the mixin package instead.
 */
public interface ItemPhysicsStateAccess {
	boolean tntsallin1client$isOnGround();

	void tntsallin1client$setOnGround(boolean onGround);

	boolean tntsallin1client$isInWater();

	void tntsallin1client$setInWater(boolean inWater);
}
