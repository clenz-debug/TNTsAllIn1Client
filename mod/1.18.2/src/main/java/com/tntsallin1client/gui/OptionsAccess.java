package com.tntsallin1client.gui;

import net.minecraft.server.packs.repository.PackRepository;

/**
 * 1.19.3's {@code Options#updateResourcePacks}, put onto 1.18.2's options the same way as
 * {@link WidgetAccess} (injected by Loom, implemented by {@code OptionsAccessMixin} with 1.19.4's code:
 * the selected packs into the options, saved, and the game's resources reloaded if the list changed).
 */
public interface OptionsAccess {
	default void updateResourcePacks(PackRepository repository) {
		throw new UnsupportedOperationException("implemented by OptionsAccessMixin");
	}
}
