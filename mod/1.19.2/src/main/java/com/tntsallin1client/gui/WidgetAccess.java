package com.tntsallin1client.gui;

/**
 * The 1.19.3+ widget methods this mod uses, put onto 1.19.2's {@code AbstractWidget} - which still has
 * public {@code x}/{@code y} fields and no tooltip of its own. Loom injects this interface into the
 * game class at compile time ({@code loom:injected_interfaces} in {@code fabric.mod.json}), and
 * {@code AbstractWidgetAccessMixin} implements every method at runtime, so the menu code stays the same
 * as in the newer versions. The defaults only exist so the compiler accepts concrete widgets; they are
 * never reached. {@code setX} and {@code setPosition} are {@link Widgets}' - see there why.
 */
public interface WidgetAccess {
	default int getX() {
		throw new UnsupportedOperationException("implemented by AbstractWidgetAccessMixin");
	}

	default int getY() {
		throw new UnsupportedOperationException("implemented by AbstractWidgetAccessMixin");
	}

	default void setY(int y) {
		throw new UnsupportedOperationException("implemented by AbstractWidgetAccessMixin");
	}

	default void setTooltip(Tooltip tooltip) {
		throw new UnsupportedOperationException("implemented by AbstractWidgetAccessMixin");
	}
}
