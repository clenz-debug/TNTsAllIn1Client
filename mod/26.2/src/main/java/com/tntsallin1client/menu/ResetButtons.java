package com.tntsallin1client.menu;

import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * What the "Reset" button on a feature's options screen and the menu's "Reset All Settings" do: ask
 * first, then reset (own user request: each mod resettable on its own, everything at once only after
 * asking). The buttons themselves are placed by {@link OptionsChrome} and {@link ClientMenuFeatures}.
 */
public final class ResetButtons {
	private ResetButtons() {
	}

	/** What a feature's "Reset" button does: asks, resets that feature on "Yes", and returns to {@code screen}. */
	public static Runnable ask(Minecraft client, Screen screen, ConfigReset.Feature feature) {
		return () -> client.gui.setScreen(confirm(client, screen,
				Component.translatable("gui.tntsallin1client.reset.confirm_title", Component.translatable(feature.labelKey)),
				Component.translatable("gui.tntsallin1client.reset.confirm_message"),
				() -> ConfigReset.reset(feature)));
	}

	/**
	 * The question before a reset. Either answer leads back to {@code returnTo}, which is laid out
	 * anew on the way and so shows the reset values.
	 *
	 * <p>In the client design the question wears that design too, see {@link ThemedConfirmScreen}.
	 */
	public static Screen confirm(Minecraft client, Screen returnTo, Component title, Component message, Runnable reset) {
		return new ThemedConfirmScreen(confirmed -> {
			if (confirmed) {
				reset.run();
			}
			client.gui.setScreen(returnTo);
		}, title, message);
	}
}
