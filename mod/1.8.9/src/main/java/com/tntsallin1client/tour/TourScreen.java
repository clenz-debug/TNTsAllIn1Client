package com.tntsallin1client.tour;

import java.util.List;

import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * What the in-game tour needs of any screen and this version keeps to the screen itself: its
 * buttons, and - for the mod's own screens - going back to where the screen was opened from.
 * Every screen is one through `ScreenMixin`.
 */
public interface TourScreen {
	List<ButtonWidget> tnt$buttons();

	/** Implemented by the mod's screens the tour closes by itself. */
	interface Closable {
		void closeForTour();
	}
}
