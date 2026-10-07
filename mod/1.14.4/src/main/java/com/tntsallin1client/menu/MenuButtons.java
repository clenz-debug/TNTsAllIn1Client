package com.tntsallin1client.menu;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * The "Client Mods" button on the title screen and in the pause menu (added by `TitleScreenMixin`
 * and `PauseScreenMixin`). The "Client Design" switch that shares its row in the other versions
 * comes with the client design.
 */
public final class MenuButtons {
	private static final int PAUSE_MENU_ROW_SPACING = 24;
	/** Same width as vanilla's Options and Quit Game buttons, so ours sits exactly under Options. */
	private static final int TITLE_BUTTON_WIDTH = 98;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TITLE_ROW_GAP = 4;

	private MenuButtons() {
	}

	/**
	 * Below the lowest vanilla button row (Options/Quit plus the language and accessibility buttons,
	 * all 20 high), under its left half - the same spot as in the other versions. Found by the
	 * buttons' own bounds rather than a fixed Y.
	 */
	public static Button forTitleScreen(Screen screen, List<AbstractWidget> buttons) {
		int lowestBottom = 0;
		for (AbstractWidget button : buttons) {
			lowestBottom = Math.max(lowestBottom, button.y + BUTTON_HEIGHT);
		}
		return openMenuButton(screen, screen.width / 2 - 100, lowestBottom + TITLE_ROW_GAP, TITLE_BUTTON_WIDTH);
	}

	/**
	 * Takes the spot of the "Disconnect"/"Save and Quit to Title" button - the lowest one - and moves
	 * that one a row down, same as in the other versions, so the mod's button isn't tacked on below
	 * everything. Null if the menu has no buttons (the game's bare pause screen).
	 */
	public static Button forPauseMenu(Screen screen, List<AbstractWidget> buttons) {
		AbstractWidget lowest = null;
		for (AbstractWidget button : buttons) {
			if (lowest == null || button.y > lowest.y) {
				lowest = button;
			}
		}
		if (lowest == null) {
			return null;
		}
		Button button = openMenuButton(screen, lowest.x, lowest.y, lowest.getWidth());
		lowest.y += PAUSE_MENU_ROW_SPACING;
		return button;
	}

	private static Button openMenuButton(Screen screen, int x, int y, int width) {
		return new Button(x, y, width, BUTTON_HEIGHT, I18n.get("gui.tntsallin1client.menu.open_button"),
				pressed -> Minecraft.getInstance().setScreen(ClientMenus.create(screen)));
	}
}
