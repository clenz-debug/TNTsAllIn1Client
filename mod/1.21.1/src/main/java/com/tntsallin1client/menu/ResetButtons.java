package com.tntsallin1client.menu;

import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
		return () -> client.setScreen(confirm(client, screen,
				Component.translatable("gui.tntsallin1client.reset.confirm_title", Component.translatable(feature.labelKey)),
				Component.translatable("gui.tntsallin1client.reset.confirm_message"),
				() -> ConfigReset.reset(feature)));
	}

	/**
	 * The question before a reset. Either answer leads back to {@code returnTo}, which is laid out
	 * anew on the way and so shows the reset values.
	 *
	 * <p>In the client design the question wears that design too (own user request, for this question
	 * only - the game's other questions keep their look): {@link Question} counts as one of the mod's
	 * own screens, which gives it the theme's background and buttons, and its two texts - drawn by the
	 * game's own text widgets, which no theme reaches - are handed over already in the client font
	 * and the theme's text color.
	 */
	public static Screen confirm(Minecraft client, Screen returnTo, Component title, Component message, Runnable reset) {
		boolean clientDesign = ClientDesign.isClient();
		return new Question(confirmed -> {
			if (confirmed) {
				reset.run();
			}
			client.setScreen(returnTo);
		}, clientDesign ? themed(title) : title, clientDesign ? themed(message) : message);
	}

	private static Component themed(Component text) {
		return ClientFont.of(text).withColor(ClientTheme.get().text & 0xFFFFFF);
	}

	/** The game's own yes/no screen under a name in this package - which is what makes it a themed screen (see {@code ThemedUi}). */
	private static final class Question extends ConfirmScreen {
		Question(BooleanConsumer callback, Component title, Component message) {
			super(callback, title, message);
		}
	}
}
