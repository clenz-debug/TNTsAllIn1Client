package com.tntsallin1client.menu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Receives {@link ClientMenuFeatures}' entries - implemented by each view of the Client Mods menu. */
public interface FeatureSink {
	/** Where a view should put a plain button - the card view gives the HUD editor its own top-bar spot ("Move/Resize"). */
	enum ButtonRole {
		HUD_EDITOR,
		OTHER
	}

	/** Starts a new section; every entry added afterwards belongs to it, until the next call. */
	void beginSection(Component label);

	void addToggleRow(boolean initial, Component label, Consumer<Boolean> onToggle, @Nullable Supplier<Screen> optionsScreenFactory);

	default void addToggleRow(boolean initial, Component label, Consumer<Boolean> onToggle) {
		addToggleRow(initial, label, onToggle, null);
	}

	void addButtonRow(ButtonRole role, Component label, Runnable onPress);
}
