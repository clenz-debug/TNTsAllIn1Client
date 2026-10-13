package com.tntsallin1client.menu;

import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/**
 * The game's yes/no question in the client design - for the questions the mod's menus ask (reset,
 * delete; own user request). The buttons and what they do are the game's. Being one of the mod's
 * menu screens (see {@code ThemedUi}) gives it the theme's background and buttons; its two texts -
 * drawn by the game's own text widgets, which no theme reaches - are handed over already in the
 * client font and the theme's text color. In the Minecraft design it is the game's own question.
 */
public class ThemedConfirmScreen extends ConfirmScreen {
	public ThemedConfirmScreen(BooleanConsumer callback, Component title, Component message) {
		super(callback, themed(title), themed(message));
	}

	private static Component themed(Component text) {
		return ClientDesign.isClient() ? ClientFont.of(text).withColor(ClientTheme.get().text & 0xFFFFFF) : text;
	}
}
