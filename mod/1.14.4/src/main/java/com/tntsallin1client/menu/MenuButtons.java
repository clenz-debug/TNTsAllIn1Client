package com.tntsallin1client.menu;

import java.util.List;
import java.util.function.Consumer;

import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.friends.FriendsBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * The "Client Mods" and "Friends" buttons on the title screen and in the pause menu, and next to
 * Client Mods the "Client Design" button that switches to the client design (added by
 * `TitleScreenMixin` and `PauseScreenMixin`).
 */
public final class MenuButtons {
	private static final int PAUSE_MENU_ROW_SPACING = 24;
	/** Same width as vanilla's Options and Quit Game buttons, so ours sits exactly under Options. */
	private static final int TITLE_BUTTON_WIDTH = 98;
	private static final int BUTTON_HEIGHT = 20;
	/** The game's gap between the two half-width buttons of a row. */
	private static final int HALF_BUTTON_GAP = 4;
	private static final int TITLE_ROW_GAP = 4;
	private static final int FRIENDS_BUTTON_MARGIN = 6;
	private static final int FRIENDS_BUTTON_WIDTH = 90;

	private MenuButtons() {
	}

	/**
	 * Below the lowest vanilla button row (Options/Quit plus the language and accessibility buttons,
	 * all 20 high), under its left half - the same spot as in the other versions. Found by the
	 * buttons' own bounds rather than a fixed Y. Next to it "Client Design", the switch to the client
	 * design ({@link TitleScreenDesign}) - both together are exactly as wide as the Options/Quit row
	 * above them.
	 */
	public static void addToTitleScreen(Screen screen, List<AbstractWidget> buttons, Consumer<AbstractWidget> add) {
		int lowestBottom = 0;
		for (AbstractWidget button : buttons) {
			lowestBottom = Math.max(lowestBottom, button.y + BUTTON_HEIGHT);
		}
		int y = lowestBottom + TITLE_ROW_GAP;
		add.accept(openMenuButton(screen, screen.width / 2 - 100, y, TITLE_BUTTON_WIDTH));
		add.accept(designButton(screen.width / 2 + 2, y, TITLE_BUTTON_WIDTH));
	}

	/**
	 * Takes the spot of the "Disconnect"/"Save and Quit to Title" button - the lowest one - and moves
	 * that one a row down, same as in the other versions, so the mod's button isn't tacked on below
	 * everything. In the Minecraft design that row is split into Client Mods and the Client Design
	 * switch, like on the title screen; in the client design (`clientLayout`) the logo is the switch
	 * and Client Mods gets the full row. Does nothing if the menu has no buttons (the game's bare
	 * pause screen).
	 */
	public static void addToPauseMenu(Screen screen, List<AbstractWidget> buttons, Consumer<AbstractWidget> add, boolean clientLayout) {
		AbstractWidget lowest = null;
		for (AbstractWidget button : buttons) {
			if (lowest == null || button.y > lowest.y) {
				lowest = button;
			}
		}
		if (lowest == null) {
			return;
		}
		int x = lowest.x;
		int y = lowest.y;
		int width = lowest.getWidth();
		lowest.y += PAUSE_MENU_ROW_SPACING;
		if (clientLayout) {
			add.accept(openMenuButton(screen, x, y, width));
			return;
		}
		int half = (width - HALF_BUTTON_GAP) / 2;
		add.accept(openMenuButton(screen, x, y, half));
		add.accept(designButton(x + width - half, y, half));
	}

	/**
	 * A small "Friends" button in the top left corner, as in the other versions - only while the
	 * launcher hands us friends data. It names the number of waiting invitations. `themed`: as a
	 * button of the client design.
	 */
	public static void addFriendsButton(Screen screen, Consumer<AbstractWidget> add, boolean themed) {
		if (!FriendsBridge.isActive()) {
			return;
		}
		Runnable open = () -> Minecraft.getInstance().setScreen(new FriendsInGameScreen(screen));
		add.accept(themed
				? new ThemedButton(FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_WIDTH, BUTTON_HEIGHT, friendsLabel(), open)
				: new Button(FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_WIDTH, BUTTON_HEIGHT, friendsLabel(), pressed -> open.run()));
	}

	/** What the Friends button says right now. */
	public static String friendsLabel() {
		int invites = FriendsBridge.invites().size();
		return invites == 0
				? I18n.get("gui.tntsallin1client.friends.title")
				: I18n.get("gui.tntsallin1client.friends.button_with_invites", invites);
	}

	private static Button openMenuButton(Screen screen, int x, int y, int width) {
		return new Button(x, y, width, BUTTON_HEIGHT, I18n.get("gui.tntsallin1client.menu.open_button"),
				pressed -> Minecraft.getInstance().setScreen(ClientMenus.create(screen)));
	}

	/** "Client Design" in the game's own layout - the logo grows out of it when the design is switched. */
	private static Button designButton(int x, int y, int width) {
		TitleScreenDesign.setMinecraftAnchor(x, y, width, BUTTON_HEIGHT);
		return new Button(x, y, width, BUTTON_HEIGHT, I18n.get("gui.tntsallin1client.design.button"), pressed -> TitleScreenDesign.switchToClient());
	}
}
