package com.tntsallin1client.zoom;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.util.math.MathHelper;

/**
 * Hold {@link ModKeyBindings#ZOOM} to temporarily narrow the field of view. The game's own settings
 * are never changed: the renderer is handed other values for as long as the key is held (see
 * {@code GameRendererMixin}), so nothing has to be put back on release and a zoomed value can never
 * end up in {@code options.txt}.
 *
 * <p>While zooming the mouse wheel changes the zoom level instead of the hotbar slot (see
 * {@code MinecraftClientMixin}); the level is saved, so the next zoom starts where the last one
 * ended. The mouse is slowed down as well - the game's turn speed doesn't take the field of view
 * into account, so at a narrow one the same hand movement would swing across far too much of it.
 */
public final class ZoomHandler {
	private static final int MIN_ZOOM_FOV = 2;
	private static final int MAX_ZOOM_FOV = 50;
	private static final int SCROLL_STEP = 2;

	private ZoomHandler() {
	}

	/** The game releases every key while a screen is open, so this is only ever true during gameplay. */
	public static boolean isZooming() {
		return ClientConfig.get().zoomEnabled && ModKeyBindings.ZOOM.isPressed();
	}

	/** The field of view to render the world with, given the one from the game's settings. */
	public static float fov(float settingsFov) {
		return isZooming() ? ClientConfig.get().zoomFov : settingsFov;
	}

	/** The mouse sensitivity to turn the player with, given the one from the game's settings. */
	public static float sensitivity(float settingsSensitivity) {
		return isZooming() ? settingsSensitivity * ClientConfig.get().zoomSensitivityPercent / 100.0F : settingsSensitivity;
	}

	/**
	 * Takes the mouse wheel for the zoom level while zooming: scrolling up narrows the field of view
	 * (zooms in further), scrolling down widens it again.
	 *
	 * @return what is left of the wheel movement for the game - nothing while zooming
	 */
	public static int handleWheel(int wheel) {
		if (wheel == 0 || !isZooming()) {
			return wheel;
		}
		ClientConfig config = ClientConfig.get();
		int newFov = MathHelper.clamp(config.zoomFov - Integer.signum(wheel) * SCROLL_STEP, MIN_ZOOM_FOV, MAX_ZOOM_FOV);
		if (newFov != config.zoomFov) {
			config.zoomFov = newFov;
			config.save();
		}
		return 0;
	}
}
