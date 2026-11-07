package com.tntsallin1client.hud;

import java.util.List;

/**
 * Phase 5m follow-up: stable identifier for each individual box in the
 * keystrokes overlay, independent of its display label (which can be renamed
 * later, like DROP's "Drop" label for what used to just be "Q") - this is
 * what {@link com.tntsallin1client.config.ClientConfig#keystrokesKeyEnabled}
 * keys off of, and what the keys on/off toggle screen and the live HUD both
 * build their row layout from, so the two can never drift out of sync.
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

	/** Same row/column grouping the overlay renders - shared with the keys toggle screen so both always match. */
	public static final List<List<KeystrokeKey>> ROWS = List.of(
			List.of(W),
			List.of(A, S, D),
			List.of(SHIFT, SPACE),
			List.of(LMB, RMB),
			List.of(SPRINT, DROP));
}
