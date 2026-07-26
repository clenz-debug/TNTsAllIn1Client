package com.tntsallin1client.zoom;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.mixin.OptionInstanceAccessor;
import net.minecraft.client.Minecraft;

/**
 * Phase 5h: hold {@link ModKeyBindings#ZOOM} to temporarily reduce the FOV.
 * {@code GameRenderer#getFov} reads {@code options.fov()} fresh every frame,
 * so overwriting that option's live value while the key is held (and
 * restoring it on release) is enough - vanilla's own smoothing/lerp on top of
 * it does the rest.
 *
 * <p><b>Bugfix (post-5h feedback):</b> the original {@code ZOOM_FOV = 15}
 * never actually did anything - {@code options.fov()}'s ValueSet is an
 * {@code OptionInstance.IntRange(30, 110)} (checked in the decompiled
 * {@code Options.java}), so a plain {@code set(15)} was silently rejected and
 * reset to the 70 default, the exact same failure mode 5j's fullbright ran
 * into with gamma's [0.0, 1.0] clamp. Fixed the same way: bypass validation
 * via {@link OptionInstanceAccessor} for the zoomed-in value. Restoring the
 * previous value on release still uses the normal, validated {@code set()} -
 * that value was always in-range to begin with, since it came from
 * {@code get()} before zooming.
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
			setFovBypassingValidation(client, ZOOM_FOV);
			zooming = true;
		} else if (!holding && zooming) {
			client.options.fov().set(previousFov);
			zooming = false;
		}
	}

	@SuppressWarnings("unchecked")
	private static void setFovBypassingValidation(Minecraft client, int value) {
		((OptionInstanceAccessor<Integer>) (Object) client.options.fov()).tntsallin1client$setValue(value);
	}
}
