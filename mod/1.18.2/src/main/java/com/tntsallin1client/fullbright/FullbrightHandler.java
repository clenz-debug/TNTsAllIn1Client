package com.tntsallin1client.fullbright;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Phase 5j: forces the world to render at full brightness regardless of the
 * actual light level, by pushing {@code options.gamma} far past the vanilla
 * slider's 0.0-1.0 range. In 1.18.2 gamma is still a plain public field (no
 * {@code OptionInstance} with a validating ValueSet before 1.19), and
 * {@code LightTexture} reads it unclamped - so it is simply written.
 * Toggle-based like the other mod-menu switches, not hold-based like zoom -
 * remembers the gamma value from just before enabling and restores exactly
 * that on disable, mirroring {@link com.tntsallin1client.zoom.ZoomHandler}'s
 * save/restore shape.
 *
 * <p>Known rough edge, not worth solving for a "leicht" feature: if the user
 * manually drags the vanilla Brightness slider while this is active, that
 * overwrites our forced value and fullbright silently stops until toggled off
 * and back on.
 *
 * <p>{@code restoreRealGammaForSave}/{@code reapplyBypassAfterSave} exist for
 * {@code OptionsSaveMixin}: {@code Options#save()} writes the field as it is, so without the swap
 * any options save while fullbright is active would put 100 into options.txt as the player's own
 * brightness (in the newer versions the bypass value is dropped from the file instead - own user
 * report from the log). Swapping back to the real value for just the duration of that one call
 * keeps options.txt always valid without weakening the bypass itself.
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
			previousGamma = client.options.gamma;
			client.options.gamma = FULLBRIGHT_GAMMA;
			active = true;
		} else if (!enabled && active) {
			client.options.gamma = previousGamma;
			active = false;
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static void restoreRealGammaForSave(Options options) {
		options.gamma = previousGamma;
	}

	public static void reapplyBypassAfterSave(Options options) {
		options.gamma = FULLBRIGHT_GAMMA;
	}
}
