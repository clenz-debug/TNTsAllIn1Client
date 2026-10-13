package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5h follow-up: while zooming, mouse wheel scroll should adjust the
 * zoom level instead of vanilla's own default behavior (changing the held
 * hotbar slot) - {@code MouseHandler#onScroll} does both the hotbar-slot
 * change and the future screen-scroll dispatch in one private method with no
 * separate extension point, so this cancels it entirely while
 * {@link ZoomHandler#isZooming()} and handles the scroll ourselves; otherwise
 * it's a no-op and vanilla's own handling runs untouched.
 *
 * <p>Freecam: {@code turnPlayer(double)} is where vanilla turns the raw accumulated mouse delta
 * into the player's look rotation - {@link com.tntsallin1client.mixin.EntityTurnMixin} cancels
 * that application to the real (frozen) player separately, so here we just also forward the same
 * raw delta to {@link FreecamHandler#rotate} while freecam is active, non-cancelling (letting
 * vanilla's own method finish harmlessly - the actual player-facing effect is what's suppressed).
 * The mouse wheel must not switch the frozen player's hotbar slot either: the only
 * {@code setSelectedSlot} call in {@code onScroll} (bytecode-verified) is skipped while freecam is
 * active - scrolling in screens and the spectator menu sit in other branches and stay as they are.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onScroll(long windowHandle, double xOffset, double yOffset, CallbackInfo ci) {
		if (ZoomHandler.isZooming()) {
			ZoomHandler.adjustZoomFov(yOffset);
			ci.cancel();
		}
	}

	@WrapWithCondition(method = "onScroll", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private boolean tntsallin1client$blockHotbarScrollWhileFreecam(Inventory inventory, int slot) {
		return !FreecamHandler.isActive();
	}

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void tntsallin1client$feedFreecamLook(double partialTick, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			FreecamHandler.rotate(Minecraft.getInstance(), this.accumulatedDX, this.accumulatedDY);
		}
	}
}
