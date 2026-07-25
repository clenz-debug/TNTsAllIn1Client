package com.tntsallin1client.menu;

import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

/**
 * Phase 5e: opens {@link ClientMenuScreen} from gameplay via keybind, and slots a
 * button into the pause menu at the spot the "Save and Quit to Title" button used
 * to occupy - that button gets pushed down instead of the mod button being tacked
 * on somewhere it'd stand out. Deliberately doesn't touch PauseScreen's private
 * layout code (fragile across versions) - it repositions the already-built button
 * via its public bounds after init, which is a much smaller surface to break.
 */
public final class PauseMenuIntegration {
	private static final int FULL_WIDTH_BUTTON = 204;

	private PauseMenuIntegration() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ModKeyBindings.OPEN_MENU.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ClientMenuScreen(null));
				}
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof PauseScreen pauseScreen) || !pauseScreen.showsPauseMenu()) {
				return;
			}

			AbstractWidget disconnectButton = findDisconnectButton(screen);
			if (disconnectButton == null) {
				return;
			}

			int x = disconnectButton.getX();
			int y = disconnectButton.getY();
			int width = disconnectButton.getWidth();
			int height = disconnectButton.getHeight();

			disconnectButton.setY(y + height + 4);

			Screens.getButtons(screen).add(Button.builder(
					Component.translatable("gui.tntsallin1client.menu.open_button"),
					button -> client.setScreen(new ClientMenuScreen(pauseScreen))
				)
				.bounds(x, y, width, height)
				.build());
		});
	}

	/**
	 * The disconnect button ("Save and Quit to Title" / "Disconnect") is the only
	 * full-width button anchored at the bottom of the pause menu grid - identified
	 * by width rather than message text/order, since both are more likely to shift
	 * between Minecraft versions than the button being full-width and lowest.
	 */
	private static AbstractWidget findDisconnectButton(net.minecraft.client.gui.screens.Screen screen) {
		AbstractWidget lowest = null;
		for (AbstractWidget widget : Screens.getButtons(screen)) {
			if (widget.getWidth() == FULL_WIDTH_BUTTON && (lowest == null || widget.getY() > lowest.getY())) {
				lowest = widget;
			}
		}
		return lowest;
	}
}
