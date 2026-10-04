package com.tntsallin1client.menu;

import java.awt.Desktop;
import java.net.URI;

import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Third-party credits: what this version of the client is built on, each with its license. A row
 * opens the project's own page - after the game's usual "do you want to open this link" question.
 *
 * <p>Scoped to what this version actually uses, which is not what the Fabric versions list: no mod
 * loader and no bundled mods here, but the libraries our own way of starting the game rests on.
 * Update the list alongside `build.gradle.kts` - nothing keeps the two in step.
 */
public class CreditsScreen extends ClientScreen {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int FIRST_ROW_Y = 32;
	private static final int BACK_BUTTON_ID = 0;
	/** A row's button gets this plus its index in {@link #ENTRIES} - the id the link question answers with, too. */
	private static final int FIRST_ROW_ID = 100;

	private static final Entry[] ENTRIES = {
			// Inside our jar.
			new Entry("Mixin", "MIT", "https://github.com/SpongePowered/Mixin"),
			new Entry("Inter (font)", "SIL OFL 1.1", "https://github.com/rsms/inter"),
			// Both come from Mojang's library server when the launcher sets the game up.
			new Entry("LaunchWrapper", "Mojang", "https://github.com/Mojang/LegacyLauncher"),
			new Entry("ASM", "BSD-3-Clause", "https://asm.ow2.io"),
			// The names the mod is written against; only used while building.
			new Entry("Legacy Yarn", "CC0-1.0", "https://github.com/Legacy-Fabric/yarn"),
	};

	public CreditsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.credits.title");
	}

	@Override
	public void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		for (int index = 0; index < ENTRIES.length; index++) {
			Entry entry = ENTRIES[index];
			this.buttons.add(new ButtonWidget(FIRST_ROW_ID + index, x, FIRST_ROW_Y + index * ROW_SPACING, ROW_WIDTH, ROW_HEIGHT,
					entry.name + " — " + entry.license));
		}
		this.buttons.add(new ButtonWidget(BACK_BUTTON_ID, x, this.height - 24, ROW_WIDTH, ROW_HEIGHT, I18n.translate("gui.back")));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK_BUTTON_ID) {
			back();
		} else if (button.id >= FIRST_ROW_ID && button.id < FIRST_ROW_ID + ENTRIES.length) {
			// Asks first; the answer comes back through confirmResult.
			this.client.setScreen(new ConfirmChatLinkScreen(this, ENTRIES[button.id - FIRST_ROW_ID].url, button.id, false));
		}
	}

	/** The answer to the link question. Either way the player is back on this screen afterwards. */
	@Override
	public void confirmResult(boolean confirmed, int id) {
		if (confirmed && id >= FIRST_ROW_ID && id < FIRST_ROW_ID + ENTRIES.length) {
			open(ENTRIES[id - FIRST_ROW_ID].url);
		}
		this.client.setScreen(this);
	}

	private static void open(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception | LinkageError e) {
			LOGGER.warn("Couldn't open the link " + url + ".", e);
		}
	}

	private static final class Entry {
		final String name;
		final String license;
		final String url;

		Entry(String name, String license, String url) {
			this.name = name;
			this.license = license;
			this.url = url;
		}
	}
}
