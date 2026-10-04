package com.tntsallin1client.menu;

import java.util.List;

import com.tntsallin1client.design.ClientDesign;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * Where an options screen's buttons besides its options go in the client design, as in the Fabric
 * versions: "Back" in the top left corner, "Move/Resize" (the HUD editor) in the top right one, and
 * "Reset" below that - or in its place where there is none. In the Minecraft design they continue
 * the screen's column instead, which each screen does itself.
 */
final class TopBar {
	static final int MARGIN = 10;
	static final int Y = 8;
	static final int BUTTON_WIDTH = 80;
	static final int BUTTON_HEIGHT = 20;
	static final int SECOND_ROW_Y = Y + BUTTON_HEIGHT + 4;

	private TopBar() {
	}

	static boolean inUse() {
		return ClientDesign.isClient();
	}

	/** The left edge of a button `width` wide in the top right corner. */
	static int rightX(int screenWidth, int width) {
		return screenWidth - MARGIN - width;
	}

	/** "Back" alone, for a screen that lays the rest out itself: in the top left corner in the client design, else where the screen has it. */
	static ButtonWidget back(int id, int x, int y, int width) {
		String label = I18n.translate("gui.back");
		return inUse() ? new ButtonWidget(id, MARGIN, Y, BUTTON_WIDTH, BUTTON_HEIGHT, label) : new ButtonWidget(id, x, y, width, BUTTON_HEIGHT, label);
	}

	/** Adds "Back" and - `hudEditor` - "Move/Resize". What a click does is the screen's business, by the ids. */
	static void add(List<ButtonWidget> buttons, int screenWidth, int backId, String backLabel, int hudEditorId, boolean hudEditor) {
		buttons.add(new ButtonWidget(backId, MARGIN, Y, BUTTON_WIDTH, BUTTON_HEIGHT, backLabel));
		if (hudEditor) {
			buttons.add(new ButtonWidget(hudEditorId, rightX(screenWidth, BUTTON_WIDTH), Y, BUTTON_WIDTH, BUTTON_HEIGHT,
					I18n.translate("gui.tntsallin1client.cards.move_resize")));
		}
	}
}
