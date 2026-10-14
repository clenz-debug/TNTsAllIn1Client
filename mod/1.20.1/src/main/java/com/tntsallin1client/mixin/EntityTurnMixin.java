package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freezes the real player's rotation while freecam is active. {@code turn(double, double)} - the
 * mouse-look application - is declared on {@code Entity} (not overridden by {@code LocalPlayer})
 * and called from {@code MouseHandler#turnPlayer} on the per-frame mouse path, entirely separate
 * from {@link LocalPlayerMixin}'s per-tick freeze - needs its own guard. Targets {@code Entity}
 * (where the method actually lives) with an identity check against the local player specifically,
 * so other entities' own {@code turn()} calls (e.g. mob AI) are never touched.
 */
@Mixin(Entity.class)
public class EntityTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$freezeRotationWhileFreecam(double yRotDelta, double xRotDelta, CallbackInfo ci) {
		if (FreecamHandler.isActive() && (Object) this == Minecraft.getInstance().player) {
			ci.cancel();
		}
	}
}
