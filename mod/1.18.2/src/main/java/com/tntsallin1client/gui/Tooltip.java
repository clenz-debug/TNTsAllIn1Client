package com.tntsallin1client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 1.18.2 stand-in for 1.19.3's {@code net.minecraft.client.gui.components.Tooltip}: a text shown while
 * the mouse is over a widget, wrapped like vanilla's (170 px). Set with {@link WidgetAccess#setTooltip}
 * and drawn by {@code AbstractWidgetAccessMixin} right after the widget.
 */
public final class Tooltip {
	private static final int MAX_WIDTH = 170;

	private final Component message;
	@Nullable
	private List<FormattedCharSequence> lines;

	private Tooltip(Component message) {
		this.message = message;
	}

	public static Tooltip create(Component message) {
		return new Tooltip(message);
	}

	public List<FormattedCharSequence> toCharSequence(Minecraft minecraft) {
		if (this.lines == null) {
			this.lines = minecraft.font.split(this.message, MAX_WIDTH);
		}
		return this.lines;
	}
}
