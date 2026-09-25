package com.tntsallin1client.menu;

import com.tntsallin1client.compat.EssentialCompat;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.design.TitleScreenLayoutAccess;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Phase 5u: adds a "Client Mods" button to the vanilla title screen, so the
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
 *
 * <p>Next to it, a "Client Design" button - the switch to the client design ({@link TitleScreenDesign}).
 * Both together are exactly as wide as the Options/Quit Game row above them (own user request).
 * Neither is added while the client design's own layout is showing, which has both built in.
 */
public final class TitleScreenIntegration {
	/** Same width and gap as vanilla's Options/Quit Game pair, so this row sits exactly under it. */
	private static final int BUTTON_WIDTH = 98;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	private TitleScreenIntegration() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof TitleScreen)) {
				return;
			}
			if (screen instanceof TitleScreenLayoutAccess access && access.tntsallin1client$isClientLayout()) {
				// The client design's own layout (TitleScreenDesign) already has its Client Mods button and design switch.
				return;
			}

			int lowestBottom = 0;
			for (AbstractWidget widget : Screens.getWidgets(screen)) {
				if (widget.getHeight() >= BUTTON_HEIGHT) {
					lowestBottom = Math.max(lowestBottom, widget.getY() + widget.getHeight());
				}
			}

			int y = lowestBottom + GAP;
			Screens.getWidgets(screen).add(Button.builder(
						Component.translatable("gui.tntsallin1client.menu.open_button"),
						button -> client.setScreen(ClientMenus.create(screen)))
					.bounds(scaledWidth / 2 - 100, y, BUTTON_WIDTH, BUTTON_HEIGHT)
					.build());

			int designX = scaledWidth / 2 + 2;
			TitleScreenDesign.setMinecraftAnchor(designX, y, BUTTON_WIDTH, BUTTON_HEIGHT);
			Screens.getWidgets(screen).add(EssentialCompat.lockDesignSwitch(Button.builder(
						Component.translatable("gui.tntsallin1client.design.button"),
						button -> TitleScreenDesign.switchToClient())
					.bounds(designX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
					.build()));
		});
	}
}
