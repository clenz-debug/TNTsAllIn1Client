package com.tntsallin1client.mixin;

import net.minecraft.client.entity.ClientAvatarState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Freecam: the cape is drawn from this state's "a tick ago" and current values - where it hangs,
 * how far the player has walked and how much it bobs ({@code AvatarRenderer#extractCapeState},
 * bytecode-verified). Vanilla only brings the old side up to date inside the player's tick, which
 * is canceled while freecam is active, and has no public way to do it from outside - see
 * {@link com.tntsallin1client.freecam.FreecamHandler}'s {@code tryEnter}.
 */
@Mixin(ClientAvatarState.class)
public interface ClientAvatarStateAccessor {
	@Accessor("xCloak")
	double tntsallin1client$getXCloak();

	@Accessor("yCloak")
	double tntsallin1client$getYCloak();

	@Accessor("zCloak")
	double tntsallin1client$getZCloak();

	@Accessor("xCloakO")
	void tntsallin1client$setXCloakO(double value);

	@Accessor("yCloakO")
	void tntsallin1client$setYCloakO(double value);

	@Accessor("zCloakO")
	void tntsallin1client$setZCloakO(double value);

	@Accessor("bob")
	float tntsallin1client$getBob();

	@Accessor("bobO")
	void tntsallin1client$setBobO(float value);

	@Accessor("walkDist")
	float tntsallin1client$getWalkDist();

	@Accessor("walkDistO")
	void tntsallin1client$setWalkDistO(float value);
}
