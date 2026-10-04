package com.tntsallin1client.menu;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.screen.Screen;

/** What {@link ClientMenuFeatures} fills: a Client Mods menu, whichever look it has. */
public interface FeatureSink {
	/** What a link is for - the card menu puts the first two into its top bar. */
	enum LinkRole {
		HUD_EDITOR, RESET_ALL, OTHER
	}

	/** A heading; what is added next belongs under it. */
	void beginSection(String labelKey);

	/** A feature's on/off switch. `optionsScreen` may be null for a feature that is nothing but its switch. */
	void addFeature(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Screen> optionsScreen);

	/** A button that opens another screen. */
	void addLink(LinkRole role, String labelKey, Supplier<Screen> screen);
}
