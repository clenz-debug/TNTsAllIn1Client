package com.tntsallin1client.menu;

import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;

/**
 * Where an options screen's buttons besides its options go in the client design, as in the other
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
		return ThemedUi.active();
	}

	/** The left edge of a button `width` wide in the top right corner. */
	static int rightX(int screenWidth, int width) {
		return screenWidth - MARGIN - width;
	}

	/** "Back" in the top left corner. */
	static Button back(String label, Runnable onPress) {
		return new Button(MARGIN, Y, BUTTON_WIDTH, BUTTON_HEIGHT, label, pressed -> onPress.run());
	}

	/** "Move/Resize" in the top right corner. */
	static Button hudEditor(int screenWidth, Runnable onPress) {
		return new Button(rightX(screenWidth, BUTTON_WIDTH), Y, BUTTON_WIDTH, BUTTON_HEIGHT,
				I18n.get("gui.tntsallin1client.cards.move_resize"), pressed -> onPress.run());
	}
}
