package com.tntsallin1client.menu;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * Third-party credits: what this version of the client is built on, each with its license. A row
 * opens the project's own page - after the game's usual "do you want to open this link" question.
 *
 * <p>Scoped to what this version actually uses, which is less than the newer versions list: of the
 * mods bundled there only Fabric API exists for this Minecraft version, of the packs Bushy
 * Vegetation. Update the list alongside `build.gradle.kts` and the bundle - nothing keeps them in
 * step.
 */
public class CreditsScreen extends ClientScreen {
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int FIRST_ROW_Y = 32;

	private static final Entry[] ENTRIES = {
			new Entry("Fabric Loader", "Apache-2.0", "https://github.com/FabricMC/fabric-loader"),
			new Entry("Fabric API", "Apache-2.0", "https://github.com/FabricMC/fabric"),
			new Entry("Bushy Vegetation (resource pack)", "BSD-3-Clause", "https://modrinth.com/resourcepack/bushy-vegetation"),
			new Entry("Inter (font)", "SIL OFL 1.1", "https://github.com/rsms/inter"),
	};

	public CreditsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.credits.title");
	}

	@Override
	protected void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		for (int index = 0; index < ENTRIES.length; index++) {
			final Entry entry = ENTRIES[index];
			this.addButton(new Button(x, FIRST_ROW_Y + index * ROW_SPACING, ROW_WIDTH, ROW_HEIGHT, entry.name + " — " + entry.license,
					pressed -> askToOpen(entry.url)));
		}
		// In the client design "Back" is in the top left corner, like on the options screens.
		this.addButton(TopBar.inUse()
				? TopBar.back(I18n.get("gui.back"), this::back)
				: new Button(x, this.height - 24, ROW_WIDTH, ROW_HEIGHT, I18n.get("gui.back"), pressed -> back()));
	}

	/** Asks first. Either way the player is back on this screen afterwards. */
	private void askToOpen(final String url) {
		this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
			if (confirmed) {
				Util.getPlatform().openUri(url);
			}
			this.minecraft.setScreen(this);
		}, url, false));
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
