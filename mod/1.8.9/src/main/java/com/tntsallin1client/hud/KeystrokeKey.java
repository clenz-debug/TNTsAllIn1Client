package com.tntsallin1client.hud;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;

/**
 * Stable identifier for each individual box in the keystrokes overlay, independent of its display
 * label - this is what {@link ClientConfig#keystrokesKeyEnabled} keys off of, and what the keys
 * on/off screen and the live HUD both build their row layout from, so the two can never drift out
 * of sync. Same names and labels as in the Fabric versions of the mod.
 */
public enum KeystrokeKey {
	W("W"),
	A("A"),
	S("S"),
	D("D"),
	SHIFT("Shift"),
	SPACE("Space"),
	LMB("LMB"),
	RMB("RMB"),
	SPRINT("Sprint"),
	DROP("Drop");

	public final String label;

	KeystrokeKey(String label) {
		this.label = label;
	}

	/** Same row/column grouping the overlay renders - shared with the keys on/off screen so both always match. */
	public static final List<List<KeystrokeKey>> ROWS = Collections.unmodifiableList(Arrays.asList(
			Collections.singletonList(W),
			Arrays.asList(A, S, D),
			Arrays.asList(SHIFT, SPACE),
			Arrays.asList(LMB, RMB),
			Arrays.asList(SPRINT, DROP)));

	/** A key the player never switched off has no entry and counts as shown. */
	public boolean isShown(ClientConfig config) {
		Boolean shown = config.keystrokesKeyEnabled.get(name());
		return shown == null || shown;
	}
}
