package com.tntsallin1client.tour;

import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

/** A rectangle in GUI-scaled screen coordinates - what the in-game tour highlights or hit-tests. */
public record TourRect(int x, int y, int width, int height) {
	public static TourRect of(AbstractWidget widget) {
		return new TourRect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
	}

	/** The bounding box of both; either may be null. */
	public static @Nullable TourRect union(@Nullable TourRect a, @Nullable TourRect b) {
		if (a == null) return b;
		if (b == null) return a;
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

	public boolean contains(double px, double py) {
		return px >= this.x && px < right() && py >= this.y && py < bottom();
	}

	public TourRect inflate(int by) {
		return new TourRect(this.x - by, this.y - by, this.width + 2 * by, this.height + 2 * by);
	}
}
