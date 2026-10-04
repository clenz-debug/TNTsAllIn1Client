package com.tntsallin1client.menu;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.IdentifiableBooleanConsumer;

/**
 * The game's yes/no question in the client design - for the questions before a reset (one feature's
 * or "Reset All"), as in the Fabric versions (own user request). The buttons and what they do are the game's; being one of the
 * mod's menu screens, it gets the theme's background and buttons by itself (`ThemedUi`), and its two
 * texts are drawn here in the client font.
 */
public class ThemedConfirmScreen extends ConfirmScreen {
	/** Where the game's own question has its two texts. */
	private static final int TITLE_Y = 70;
	private static final int MESSAGE_Y = 90;
	private static final int LINE_HEIGHT = 10;
	private static final int SIDE_MARGIN = 25;

	private final String question;
	private final String message;

	public ThemedConfirmScreen(IdentifiableBooleanConsumer consumer, String question, String message, int id) {
		super(consumer, question, message, id);
		this.question = question;
		this.message = message;
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		this.renderBackground();
		MenuText.centered(this.question, this.width / 2, TITLE_Y, 0xFFFFFF);
		int y = MESSAGE_Y;
		for (String line : MenuText.wrap(this.message, this.width - 2 * SIDE_MARGIN)) {
			MenuText.centered(line, this.width / 2, y, 0xFFFFFF);
			y += LINE_HEIGHT;
		}
		for (ButtonWidget button : this.buttons) {
			button.render(this.client, mouseX, mouseY);
		}
	}
}
