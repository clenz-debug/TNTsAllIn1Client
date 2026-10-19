package com.tntsallin1client.zoom;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;

/**
 * Phase 5h: hold {@link ModKeyBindings#ZOOM} to temporarily reduce the FOV.
 * {@code GameRenderer#getFov} reads {@code options.fov} fresh every frame,
 * so overwriting that option's live value while the key is held (and
 * restoring it on release) is enough - vanilla's own smoothing/lerp on top of
 * it does the rest. In 1.18.2 the FOV is still a plain public field (no
 * {@code OptionInstance} with a validating ValueSet before 1.19), so the
 * zoomed-in value is simply written.
 *
 * <p><b>Scroll-to-adjust (follow-up feedback):</b> {@link com.tntsallin1client.mixin.MouseHandlerMixin}
 * cancels vanilla's own scroll handling (which would otherwise change the
 * held hotbar slot) while zooming and calls {@link #adjustZoomFov(double)}
 * instead. The resulting FOV, {@link ClientConfig#zoomFov}, is saved like any
 * other setting, so the chosen zoom level is remembered next time zoom is
 * pressed rather than resetting every time.
 *
 * <p><b>Sensitivity reduction (follow-up feedback):</b> {@code MouseHandler#turnPlayer}
 * reads {@code options.sensitivity} but never accounts for FOV, so the same
 * mouse movement that used to pan a fraction of the normal view now swings
 * across a much larger share of the narrow zoomed view - it feels like
 * sensitivity spiked. Fixed the same way as the FOV itself: scale sensitivity
 * down by {@link ClientConfig#zoomSensitivityPercent} while the key is held,
 * restore the original value on release.
 */
public final class ZoomHandler {
	private static final int MIN_ZOOM_FOV = 2;
	private static final int MAX_ZOOM_FOV = 50;
	private static final int SCROLL_STEP = 2;

	private static boolean zooming = false;
	private static double previousFov;
	private static double previousSensitivity;

	private ZoomHandler() {
	}

	public static void tick(Minecraft client) {
		boolean holding = ClientConfig.get().zoomEnabled && ModKeyBindings.ZOOM.isDown();
		if (holding && !zooming) {
			previousFov = client.options.fov;
			client.options.fov = ClientConfig.get().zoomFov;
			previousSensitivity = client.options.sensitivity;
			client.options.sensitivity = previousSensitivity * ClientConfig.get().zoomSensitivityPercent / 100.0;
			zooming = true;
		} else if (!holding && zooming) {
			client.options.fov = previousFov;
			client.options.sensitivity = previousSensitivity;
			zooming = false;
		}
	}

	public static boolean isZooming() {
		return zooming;
	}

	/**
	 * For {@code OptionsSaveMixin}: {@code Options#save()} writes the plain fields as they are, so an
	 * options save while zooming (F11 saves, for one) would put the zoomed FOV and the reduced
	 * sensitivity into options.txt as the player's own values.
	 */
	public static void restoreRealValuesForSave(Options options) {
		options.fov = previousFov;
		options.sensitivity = previousSensitivity;
	}

	public static void reapplyZoomAfterSave(Options options) {
		options.fov = ClientConfig.get().zoomFov;
		options.sensitivity = previousSensitivity * ClientConfig.get().zoomSensitivityPercent / 100.0;
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
			Minecraft.getInstance().options.fov = newFov;
		}
	}
}
