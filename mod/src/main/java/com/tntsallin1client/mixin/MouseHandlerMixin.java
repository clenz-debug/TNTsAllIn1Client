package com.tntsallin1client.mixin;

import com.tntsallin1client.zoom.ZoomHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
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
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onScroll(long windowHandle, double xOffset, double yOffset, CallbackInfo ci) {
		if (ZoomHandler.isZooming()) {
			ZoomHandler.adjustZoomFov(yOffset);
			ci.cancel();
		}
	}
}
