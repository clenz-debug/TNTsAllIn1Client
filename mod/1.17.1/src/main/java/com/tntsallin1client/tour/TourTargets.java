package com.tntsallin1client.tour;

import org.jetbrains.annotations.Nullable;

/**
 * Screens that draw their own content (the Client Mods list and cards) tell the in-game tour where
 * a named part of them is - plain widgets are found by their label instead ({@link InGameTourSteps}).
 */
public interface TourTargets {
	String SEARCH = "search";
	String HUD_EDITOR = "hud_editor";
	/** Client design only: Done, search and Move/Resize along the top. */
	String TOP_BAR = "top_bar";
	/** Followed by a feature's menu translation key: the whole row/card. */
	String FEATURE = "feature:";
	/** Followed by a feature's menu translation key: its Options button/strip. */
	String FEATURE_OPTIONS = "feature_options:";
	/** Followed by a feature's menu translation key: its on/off switch (toggle / Enabled bar). */
	String FEATURE_SWITCH = "feature_switch:";

	/** Where that part is on screen right now, or null if it isn't visible (a screen may scroll it into view for the next frame). */
	@Nullable TourRect tourTarget(String name);
}
