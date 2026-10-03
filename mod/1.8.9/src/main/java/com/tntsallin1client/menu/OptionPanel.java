package com.tntsallin1client.menu;

/**
 * A row of a {@link FeatureOptionsScreen} that is not a button but an area drawing itself - a
 * preview, a grid to paint on. The screen tells it where it is each time: the area is `width` wide,
 * {@link #height()} tall and has its top left corner at `x`/`y`, which moves as the screen scrolls.
 */
interface OptionPanel {
	int height();

	void render(int x, int y, int width);

	/** @return whether the click was on something of the panel and is dealt with */
	default boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		return false;
	}

	/** Called for every mouse movement with a button held, wherever the click started. */
	default void mouseDragged(int x, int y, int width, int mouseX, int mouseY) {
	}

	default void mouseReleased() {
	}
}
