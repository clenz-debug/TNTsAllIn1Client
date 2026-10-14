package com.tntsallin1client.zoom;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.mixin.OptionInstanceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Phase 5h: hold {@link ModKeyBindings#ZOOM} to temporarily reduce the FOV.
 * {@code GameRenderer#getFov} reads {@code options.fov()} fresh every frame,
 * so overwriting that option's live value while the key is held (and
 * restoring it on release) is enough - vanilla's own smoothing/lerp on top of
 * it does the rest.
 *
 * <p><b>Bugfix (post-5h feedback):</b> the original hardcoded zoom FOV never
 * actually did anything - {@code options.fov()}'s ValueSet is an
 * {@code OptionInstance.IntRange(30, 110)} (checked in the decompiled
 * {@code Options.java}), so a plain {@code set(15)} was silently rejected and
 * reset to the 70 default, the exact same failure mode 5j's fullbright ran
 * into with gamma's [0.0, 1.0] clamp. Fixed the same way: bypass validation
 * via {@link OptionInstanceAccessor} for the zoomed-in value. Restoring the
 * previous value on release still uses the normal, validated {@code set()} -
 * that value was always in-range to begin with, since it came from
 * {@code get()} before zooming.
 *
 * <p><b>Scroll-to-adjust (follow-up feedback):</b> {@link com.tntsallin1client.mixin.MouseHandlerMixin}
 * cancels vanilla's own scroll handling (which would otherwise change the
 * held hotbar slot) while zooming and calls {@link #adjustZoomFov(double)}
 * instead. The resulting FOV, {@link ClientConfig#zoomFov}, is saved like any
 * other setting, so the chosen zoom level is remembered next time zoom is
 * pressed rather than resetting every time.
 *
 * <p><b>Sensitivity reduction (follow-up feedback):</b> {@code MouseHandler#turnPlayer}
 * reads {@code options.sensitivity()} but never accounts for FOV, so the same
 * mouse movement that used to pan a fraction of the normal view now swings
 * across a much larger share of the narrow zoomed view - it feels like
 * sensitivity spiked. Fixed the same way as the FOV itself: scale sensitivity
 * down by {@link ClientConfig#zoomSensitivityPercent} while the key is held,
 * restore the original value on release. Unlike FOV's [30, 110] IntRange,
 * sensitivity's ValueSet is a plain {@code UnitDouble} ([0.0, 1.0]) and the
 * scaled-down value is always within that range, so the normal validated
 * {@code set()} works here - no {@link OptionInstanceAccessor} bypass needed.
 */
public final class ZoomHandler {
	private static final int MIN_ZOOM_FOV = 2;
	private static final int MAX_ZOOM_FOV = 50;
	private static final int SCROLL_STEP = 2;

	private static boolean zooming = false;
	private static int previousFov;
	private static double previousSensitivity;

	private ZoomHandler() {
	}

	public static void tick(Minecraft client) {
		boolean holding = ClientConfig.get().zoomEnabled && ModKeyBindings.ZOOM.isDown();
		if (holding && !zooming) {
			previousFov = client.options.fov().get();
			setFovBypassingValidation(client, ClientConfig.get().zoomFov);
			previousSensitivity = client.options.sensitivity().get();
			client.options.sensitivity().set(previousSensitivity * ClientConfig.get().zoomSensitivityPercent / 100.0);
			zooming = true;
		} else if (!holding && zooming) {
			client.options.fov().set(previousFov);
			client.options.sensitivity().set(previousSensitivity);
			zooming = false;
		}
	}

	public static boolean isZooming() {
		return zooming;
	}

	/** Scroll up narrows the FOV (zooms in further), scroll down widens it back out. */
	public static void adjustZoomFov(double scrollDelta) {
		if (!zooming || scrollDelta == 0) {
			return;
		}

		ClientConfig config = ClientConfig.get();
		int newFov = Mth.clamp(config.zoomFov - (int) Math.signum(scrollDelta) * SCROLL_STEP, MIN_ZOOM_FOV, MAX_ZOOM_FOV);
		if (newFov != config.zoomFov) {
			config.zoomFov = newFov;
			config.save();
			setFovBypassingValidation(Minecraft.getInstance(), newFov);
		}
	}

	@SuppressWarnings("unchecked")
	private static void setFovBypassingValidation(Minecraft client, int value) {
		((OptionInstanceAccessor<Integer>) (Object) client.options.fov()).tntsallin1client$setValue(value);
	}
}
