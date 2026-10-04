package com.tntsallin1client.tour;

import com.tntsallin1client.design.ThemedButton;
import net.minecraft.client.gui.widget.ButtonWidget;

/** A rectangle in GUI-scaled screen coordinates - what the in-game tour highlights or hit-tests. */
public final class TourRect {
	/** The height of every button of the game's that the tour points at - a button doesn't tell its own. */
	private static final int BUTTON_HEIGHT = 20;

	public final int x;
	public final int y;
	public final int width;
	public final int height;

	public TourRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public static TourRect of(ButtonWidget button) {
		int height = button instanceof ThemedButton ? ((ThemedButton) button).getHeight() : BUTTON_HEIGHT;
		return new TourRect(button.x, button.y, button.getWidth(), height);
	}

	/** The bounding box of both; either may be null. */
	public static TourRect union(TourRect a, TourRect b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		int left = Math.min(a.x, b.x);
		int top = Math.min(a.y, b.y);
		return new TourRect(left, top, Math.max(a.right(), b.right()) - left, Math.max(a.bottom(), b.bottom()) - top);
	}

	public int right() {
		return this.x + this.width;
	}

	public int bottom() {
		return this.y + this.height;
	}

	public boolean contains(int px, int py) {
		return px >= this.x && px < right() && py >= this.y && py < bottom();
	}

	public TourRect inflate(int by) {
		return new TourRect(this.x - by, this.y - by, this.width + 2 * by, this.height + 2 * by);
	}
}
