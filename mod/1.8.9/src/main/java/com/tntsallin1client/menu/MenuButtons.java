package com.tntsallin1client.menu;

import java.util.List;

import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.friends.FriendsBridge;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * The "Client Mods" and "Friends" buttons on the title screen and in the pause menu, and the title
 * screen's "Client Design" button (added by `TitleScreenMixin` and `GameMenuScreenMixin`).
 */
public final class MenuButtons {
	/** Vanilla numbers its buttons from 0 upwards per screen - far away from those. */
	public static final int OPEN_MENU_BUTTON_ID = 7140;
	public static final int FRIENDS_BUTTON_ID = 7141;
	/** The design switch: "Client Design" in the game's own layout, the logo in the client design's. */
	public static final int DESIGN_BUTTON_ID = 7142;
	private static final int FRIENDS_BUTTON_MARGIN = 6;
	private static final int FRIENDS_BUTTON_WIDTH = 90;

	private static final int PAUSE_MENU_DISCONNECT_BUTTON_ID = 1;
	private static final int PAUSE_MENU_ROW_SPACING = 24;
	/** Same width as vanilla's Options and Quit Game buttons, so ours sits exactly under Options. */
	private static final int TITLE_BUTTON_WIDTH = 98;
	public static final int BUTTON_HEIGHT = 20;
	/** The game's gap between the two half-width buttons of a row. */
	private static final int HALF_BUTTON_GAP = 4;
	private static final int TITLE_ROW_GAP = 4;

	private MenuButtons() {
	}

	/**
	 * Below the lowest vanilla button row (Options/Quit plus the language button, all 20 high),
	 * under its left half - the same spot as in the Fabric versions. Found by the buttons' own
	 * bounds rather than a fixed Y. Next to it "Client Design", the switch to the client design
	 * ({@link TitleScreenDesign}) - both together are exactly as wide as the Options/Quit row above them.
	 */
	public static void addToTitleScreen(List<ButtonWidget> buttons, int screenWidth) {
		int lowestBottom = 0;
		for (ButtonWidget button : buttons) {
			lowestBottom = Math.max(lowestBottom, button.y + BUTTON_HEIGHT);
		}
		int y = lowestBottom + TITLE_ROW_GAP;
		buttons.add(new ButtonWidget(OPEN_MENU_BUTTON_ID, screenWidth / 2 - 100, y, TITLE_BUTTON_WIDTH, BUTTON_HEIGHT, label()));
		int designX = screenWidth / 2 + 2;
		TitleScreenDesign.setMinecraftAnchor(designX, y, TITLE_BUTTON_WIDTH, BUTTON_HEIGHT);
		buttons.add(new ButtonWidget(DESIGN_BUTTON_ID, designX, y, TITLE_BUTTON_WIDTH, BUTTON_HEIGHT, I18n.translate("gui.tntsallin1client.design.button")));
	}

	/**
	 * Takes the spot of the "Disconnect"/"Save and Quit to Title" button and moves that one a row
	 * down, same as in the Fabric versions - so the mod's button isn't tacked on below everything.
	 * In the Minecraft design that row is split into Client Mods and the Client Design switch, like
	 * on the title screen; in the client design (`clientLayout`) the logo is the switch and Client
	 * Mods gets the full row.
	 */
	public static void addToPauseMenu(List<ButtonWidget> buttons, boolean clientLayout) {
		for (ButtonWidget button : buttons) {
			if (button.id == PAUSE_MENU_DISCONNECT_BUTTON_ID) {
				int x = button.x;
				int y = button.y;
				int width = button.getWidth();
				button.y += PAUSE_MENU_ROW_SPACING;
				if (clientLayout) {
					buttons.add(new ButtonWidget(OPEN_MENU_BUTTON_ID, x, y, width, BUTTON_HEIGHT, label()));
					return;
				}
				int half = (width - HALF_BUTTON_GAP) / 2;
				int designX = x + width - half;
				buttons.add(new ButtonWidget(OPEN_MENU_BUTTON_ID, x, y, half, BUTTON_HEIGHT, label()));
				TitleScreenDesign.setMinecraftAnchor(designX, y, half, BUTTON_HEIGHT);
				buttons.add(new ButtonWidget(DESIGN_BUTTON_ID, designX, y, half, BUTTON_HEIGHT, I18n.translate("gui.tntsallin1client.design.button")));
				return;
			}
		}
	}

	/**
	 * A small "Friends" button in the top left corner, as in the Fabric versions - only while the
	 * launcher hands us friends data. It names the number of waiting invitations.
	 */
	public static void addFriendsButton(List<ButtonWidget> buttons) {
		addFriendsButton(buttons, false);
	}

	/** `themed`: as a button of the client design. */
	public static void addFriendsButton(List<ButtonWidget> buttons, boolean themed) {
		if (!FriendsBridge.isActive()) {
			return;
		}
		int invites = FriendsBridge.invites().size();
		String label = invites == 0
				? I18n.translate("gui.tntsallin1client.friends.title")
				: I18n.translate("gui.tntsallin1client.friends.button_with_invites", invites);
		buttons.add(themed
				? new ThemedButton(FRIENDS_BUTTON_ID, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_WIDTH, BUTTON_HEIGHT, label)
				: new ButtonWidget(FRIENDS_BUTTON_ID, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_MARGIN, FRIENDS_BUTTON_WIDTH, BUTTON_HEIGHT, label));
	}

	private static String label() {
		return I18n.translate("gui.tntsallin1client.menu.open_button");
	}
}
