package com.tntsallin1client.fullbright;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.mixin.OptionInstanceAccessor;
import net.minecraft.client.Minecraft;

/**
 * Phase 5j: forces the world to render at full brightness regardless of the
 * actual light level, by pushing {@code options.gamma()} far past the vanilla
 * slider's 0.0-1.0 range via {@link OptionInstanceAccessor} (a plain
 * {@code set()} call would get silently clamped back to the 0.5 default -
 * verified against the decompiled {@code OptionInstance.UnitDouble} source).
 * Toggle-based like the other mod-menu switches, not hold-based like zoom -
 * remembers the gamma value from just before enabling and restores exactly
 * that on disable, mirroring {@link com.tntsallin1client.zoom.ZoomHandler}'s
 * save/restore shape.
 *
 * <p>Known rough edge, not worth solving for a "leicht" feature: if the user
 * manually drags the vanilla Brightness slider while this is active, that
 * legitimate {@code set()} call overwrites our forced value and fullbright
 * silently stops until toggled off and back on.
 */
public final class FullbrightHandler {
	private static final double FULLBRIGHT_GAMMA = 100.0;

	private static boolean active = false;
	private static double previousGamma;

	private FullbrightHandler() {
	}

	public static void tick(Minecraft client) {
		boolean enabled = ClientConfig.get().fullbrightEnabled;
		if (enabled && !active) {
			previousGamma = client.options.gamma().get();
			setGammaBypassingValidation(client, FULLBRIGHT_GAMMA);
			active = true;
		} else if (!enabled && active) {
			client.options.gamma().set(previousGamma);
			active = false;
		}
	}

	@SuppressWarnings("unchecked")
	private static void setGammaBypassingValidation(Minecraft client, double value) {
		((OptionInstanceAccessor<Double>) (Object) client.options.gamma()).tntsallin1client$setValue(value);
	}
}
