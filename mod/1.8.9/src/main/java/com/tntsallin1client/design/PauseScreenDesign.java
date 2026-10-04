package com.tntsallin1client.design;

import java.util.List;
import java.util.TreeSet;

import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * The client design's pause menu, as in the Fabric versions: the game's own pause menu buttons,
 * restyled ({@link ThemedUi}) and moved down under the client logo, which replaces the "Game Menu"
 * heading and doubles as the design switch. Every button keeps its action, and every screen behind
 * them stays the game's. Switching runs the title screen's animation ({@link TitleScreenDesign}).
 *
 * <p>This version's pause menu leaves a row free in the middle of its buttons; the client layout
 * closes it (own user feedback: the gap was too big).
 */
public final class PauseScreenDesign {
	private static final int LOGO_GAP = 14;
	private static final float MIN_LOGO_HEIGHT = 30;
	private static final float MAX_LOGO_HEIGHT = 90;
	/** From one row of buttons to the next in the client layout. */
	private static final int ROW_SPACING = 24;

	/** Height of the pause menu's block of buttons, measured on each layout - the logo gets whatever room it leaves. */
	private static int gridHeight = 150;

	private PauseScreenDesign() {
	}

	/** The logo's spot in the client layout - also the switch animation's target from the game's own layout. */
	public static TitleScreenDesign.Rect clientLogoRect(int width, int height) {
		float logoHeight = Math.max(MIN_LOGO_HEIGHT, Math.min(MAX_LOGO_HEIGHT, height - gridHeight - LOGO_GAP - 24));
		float top = Math.max(6, (height - logoHeight - LOGO_GAP - gridHeight) / 2);
		return TitleScreenDesign.Rect.centered(width / 2.0F, top, logoHeight);
	}

	/** The game's own layout: only measures the block of buttons as the client layout stacks it, for where the animation's logo ends up. */
	public static void measure(List<ButtonWidget> buttons) {
		int rows = rowTops(buttons).size();
		if (rows > 0) {
			gridHeight = (rows - 1) * ROW_SPACING + MenuButtons.BUTTON_HEIGHT;
		}
	}

	private static TreeSet<Integer> rowTops(List<ButtonWidget> buttons) {
		TreeSet<Integer> tops = new TreeSet<Integer>();
		for (ButtonWidget button : buttons) {
			tops.add(button.y);
		}
		return tops;
	}

	/**
	 * Client layout: stacks the rows of buttons one right below the other under the logo and adds the
	 * logo button. Called before the "Friends" button is added, which stays in its corner.
	 */
	public static void buildClientLayout(List<ButtonWidget> buttons, int width, int height) {
		measure(buttons);
		TitleScreenDesign.Rect logo = clientLogoRect(width, height);
		int gridTop = Math.round(logo.y + logo.height) + LOGO_GAP;
		TreeSet<Integer> tops = rowTops(buttons);
		for (ButtonWidget button : buttons) {
			// The row's place among the rows, counted from the top.
			button.y = gridTop + tops.headSet(button.y).size() * ROW_SPACING;
		}
		buttons.add(new LogoButton(MenuButtons.DESIGN_BUTTON_ID, Math.round(logo.x), Math.round(logo.y), Math.round(logo.height)));
	}
}
