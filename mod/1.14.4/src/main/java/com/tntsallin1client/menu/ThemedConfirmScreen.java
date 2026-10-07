package com.tntsallin1client.menu;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.TextComponent;

/**
 * The game's yes/no question in the client design - for the questions the mod's menus ask (reset,
 * delete), as in the other versions (own user request). The buttons and what they do are the
 * game's; being one of the mod's menu screens, it gets the theme's background and buttons by itself
 * (`ThemedUi`), and its two texts are drawn here, through {@link MenuText} - in the Minecraft design
 * it looks like the game's own question.
 */
public class ThemedConfirmScreen extends ConfirmScreen {
	/** Where the game's own question has its two texts. */
	private static final int TITLE_Y = 70;
	private static final int MESSAGE_Y = 90;
	private static final int LINE_HEIGHT = 10;
	private static final int SIDE_MARGIN = 25;

	private final String question;
	private final String message;

	public ThemedConfirmScreen(BooleanConsumer callback, String question, String message) {
		super(callback, new TextComponent(question), new TextComponent(message));
		this.question = question;
		this.message = message;
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		this.renderBackground();
		MenuText.centered(this.question, this.width / 2, TITLE_Y, 0xFFFFFF);
		int y = MESSAGE_Y;
		for (String line : MenuText.wrap(this.message, this.width - 2 * SIDE_MARGIN)) {
			MenuText.centered(line, this.width / 2, y, 0xFFFFFF);
			y += LINE_HEIGHT;
		}
		for (AbstractWidget button : this.buttons) {
			button.render(mouseX, mouseY, partialTick);
		}
	}
}
