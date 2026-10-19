package com.tntsallin1client.gui;

import net.minecraft.network.chat.Component;

/**
 * 1.19.4's {@code EditBox#setHint}, put onto 1.18.2's text field the same way as {@link WidgetAccess}
 * (injected by Loom, implemented by {@code EditBoxHintMixin}): a placeholder text shown
 * while the field is empty and not focused, where the typed text would start.
 */
public interface EditBoxAccess {
	default void setHint(Component hint) {
		throw new UnsupportedOperationException("implemented by EditBoxHintMixin");
	}
}
