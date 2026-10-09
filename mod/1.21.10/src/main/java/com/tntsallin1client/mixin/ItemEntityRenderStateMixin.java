package com.tntsallin1client.mixin;

import com.tntsallin1client.itemphysics.ItemPhysicsStateAccess;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** See {@link ItemPhysicsStateAccess}. */
@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemPhysicsStateAccess {
	@Unique
	private boolean onGround;
	@Unique
	private boolean inWater;

	@Override
	public boolean tntsallin1client$isOnGround() {
		return this.onGround;
	}

	@Override
	public void tntsallin1client$setOnGround(boolean onGround) {
		this.onGround = onGround;
	}

	@Override
	public boolean tntsallin1client$isInWater() {
		return this.inWater;
	}

	@Override
	public void tntsallin1client$setInWater(boolean inWater) {
		this.inWater = inWater;
	}
}
