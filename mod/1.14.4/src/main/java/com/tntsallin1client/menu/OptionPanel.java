package com.tntsallin1client.menu;

/**
 * A row of a {@link FeatureOptionsScreen} that is not a button but an area drawing itself - a
 * preview, a grid to paint on. The screen tells it where it is each time: the area is `width` wide,
 * {@link #height()} tall and has its top left corner at `x`/`y`, which moves as the screen scrolls.
 */
interface OptionPanel {
	int height();

	/** How wide the panel draws itself in a column of the given width - it is centered on the column if wider. */
	default int width(int columnWidth) {
		return columnWidth;
	}

	/** `mouseX`/`mouseY`: where the cursor is, for what lights up under it - off screen while it is outside the scrolling area. */
	void render(int x, int y, int width, int mouseX, int mouseY);

	/** @return whether the click was on something of the panel and is dealt with */
	default boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		return false;
	}

	/** Called for every mouse movement with a button held, wherever the click started. */
	default void mouseDragged(int x, int y, int width, int mouseX, int mouseY) {
	}

	default void mouseReleased() {
	}

	/** Called before every click on the screen is handed out - a panel with keyboard focus gives it up. */
	default void unfocus() {
	}

	/** A key that is not a character: arrows, backspace, ... @return whether the panel had the keyboard focus and took the key */
	default boolean keyPressed(int key, int scanCode, int modifiers) {
		return false;
	}

	/** A typed character. @return whether the panel had the keyboard focus and took it */
	default boolean charTyped(char character, int modifiers) {
		return false;
	}

	/** Called after the settings were changed from outside (reset) - a panel holding a copy of one reads it anew. */
	default void reload() {
	}

	/** Called once per game tick. */
	default void tick() {
	}
}
