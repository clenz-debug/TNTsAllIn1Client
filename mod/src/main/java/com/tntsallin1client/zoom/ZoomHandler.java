package com.tntsallin1client.zoom;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;

/**
 * Phase 5h: hold {@link ModKeyBindings#ZOOM} to temporarily reduce the FOV.
 * No mixin needed - {@code GameRenderer#getFov} reads {@code options.fov()}
 * fresh every frame, so overwriting that option's live value while the key is
 * held (and restoring it on release) is enough; vanilla's own smoothing/lerp
 * on top of it does the rest. Never calls {@code Options#save()}, so this
 * never touches options.txt.
 */
public final class ZoomHandler {
	private static final int ZOOM_FOV = 15;

	private static boolean zooming = false;
	private static int previousFov;

	private ZoomHandler() {
	}

	public static void tick(Minecraft client) {
		boolean holding = ClientConfig.get().zoomEnabled && ModKeyBindings.ZOOM.isDown();
		if (holding && !zooming) {
			previousFov = client.options.fov().get();
			client.options.fov().set(ZOOM_FOV);
			zooming = true;
		} else if (!holding && zooming) {
			client.options.fov().set(previousFov);
			zooming = false;
		}
	}
}
