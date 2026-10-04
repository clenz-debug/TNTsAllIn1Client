package com.tntsallin1client.menu;

import java.util.List;

import com.tntsallin1client.friends.FriendsBridge;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * The "Client Mods" and "Friends" buttons on the title screen and in the pause menu (added by
 * `TitleScreenMixin` and `GameMenuScreenMixin`).
 */
public final class MenuButtons {
	/** Vanilla numbers its buttons from 0 upwards per screen - far away from those. */
	public static final int OPEN_MENU_BUTTON_ID = 7140;
	public static final int FRIENDS_BUTTON_ID = 7141;
	private static final int FRIENDS_BUTTON_MARGIN = 6;
	private static final int FRIENDS_BUTTON_WIDTH = 90;

	private static final int PAUSE_MENU_DISCONNECT_BUTTON_ID = 1;
	private static final int PAUSE_MENU_ROW_SPACING = 24;
	/** Same width as vanilla's Options and Quit Game buttons, so ours sits exactly under Options. */
	private static final int TITLE_BUTTON_WIDTH = 98;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TITLE_ROW_GAP = 4;

	private MenuButtons() {
	}

	/**
	 * Below the lowest vanilla button row (Options/Quit plus the language button, all 20 high),
	 * under its left half - the same spot as in the Fabric versions. Found by the buttons' own
	 * bounds rather than a fixed Y. Half width on purpose: at the default window size this row is
	 * level with the version and copyright lines, and the left half sits between the two.
	 */
	public static void addToTitleScreen(List<ButtonWidget> buttons, int screenWidth) {
		int lowestBottom = 0;
		for (ButtonWidget button : buttons) {
			lowestBottom = Math.max(lowestBottom, button.y + BUTTON_HEIGHT);
		}
		buttons.add(new ButtonWidget(OPEN_MENU_BUTTON_ID, screenWidth / 2 - 100, lowestBottom + TITLE_ROW_GAP, TITLE_BUTTON_WIDTH, BUTTON_HEIGHT, label()));
	}

	/**
	 * Takes the spot of the "Disconnect"/"Save and Quit to Title" button and moves that one a row
	 * down, same as in the Fabric versions - so the mod's button isn't tacked on below everything.
	 */
	public static void addToPauseMenu(List<ButtonWidget> buttons) {
		for (ButtonWidget button : buttons) {
			if (button.id == PAUSE_MENU_DISCONNECT_BUTTON_ID) {
				buttons.add(new ButtonWidget(OPEN_MENU_BUTTON_ID, button.x, button.y, button.getWidth(), BUTTON_HEIGHT, label()));
				button.y += PAUSE_MENU_ROW_SPACING;
				return;
			}
		}
	}

	/**
	 * A small "Friends" button in the top left corner, as in the Fabric versions - only while the
	 * launcher hands us friends data. It names the number of waiting invitations.
	 */
	public static void addFriendsButton(List<ButtonWidget> buttons) {
		if (!FriendsBridge.isActive()) {
			return;
		}
		int invites = FriendsBridge.invites().size();
		String label = invites == 0
				? I18n.translate("gui.tntsallin1client.friends.title")
				: I18n.translate("gui.tntsallin1client.friends.button_with_invites", invites);
		buttons.add(new ButtonWidget(FRIENDS_BUTTON_ID, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_WIDTH, BUTTON_HEIGHT, label));
	}

	private static String label() {
		return I18n.translate("gui.tntsallin1client.menu.open_button");
	}
}
