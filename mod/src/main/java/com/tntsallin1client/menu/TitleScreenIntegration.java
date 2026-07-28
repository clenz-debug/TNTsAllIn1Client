package com.tntsallin1client.menu;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Phase 5u: adds a "Mod Menu" button to the vanilla title screen, so the
 * mod's settings are reachable before joining a world too - not just from
 * the pause menu / in-game keybind ({@link PauseMenuIntegration}). Placed
 * below the lowest vanilla button row (Options/Quit plus the language and
 * accessibility icon buttons, all height 20) rather than a hardcoded Y, same
 * "find it by its own bounds instead of a fixed position" reasoning
 * {@link PauseMenuIntegration} already uses for the pause menu - more
 * robust across GUI scales and any future vanilla layout tweaks. The
 * bottom-right copyright/credits link is deliberately excluded from that
 * search (it's only 10px tall and sits right at the screen edge - including
 * it would push this button off-screen).
 */
public final class TitleScreenIntegration {
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	private TitleScreenIntegration() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof TitleScreen)) {
				return;
			}

			int lowestBottom = 0;
			for (AbstractWidget widget : Screens.getButtons(screen)) {
				if (widget.getHeight() >= BUTTON_HEIGHT) {
					lowestBottom = Math.max(lowestBottom, widget.getY() + widget.getHeight());
				}
			}

			Screens.getButtons(screen).add(Button.builder(
						Component.translatable("gui.tntsallin1client.menu.open_button"),
						button -> client.setScreen(new ClientMenuScreen(screen)))
					.bounds((scaledWidth - BUTTON_WIDTH) / 2, lowestBottom + GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
					.build());
		});
	}
}
