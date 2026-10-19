package com.tntsallin1client.gui;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Widget moves for 1.18.2, where a widget's position is its public {@code x}/{@code y} fields. Not in
 * {@link WidgetAccess}: vanilla 1.18.2 already has {@code setPosition} on three widgets and
 * {@code setX} on the text field under their own obfuscated names, which the remapper cannot unite with
 * an injected method of the same name.
 */
public final class Widgets {
	private Widgets() {
	}

	public static void setPosition(AbstractWidget widget, int x, int y) {
		widget.x = x;
		widget.y = y;
	}

	public static void setX(AbstractWidget widget, int x) {
		widget.x = x;
	}
}
