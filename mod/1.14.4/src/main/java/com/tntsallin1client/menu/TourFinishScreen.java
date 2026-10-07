package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.design.ClientDesign;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * The in-game tour's last screen, as in the other versions: which design to keep, now that the
 * tour showed both - asked only here at the end. Also the reminder that the tour's world can simply
 * be deleted again. Can't be closed with Escape, the choice is the point of it.
 */
public class TourFinishScreen extends Screen {
	private static final String KEY = "gui.tntsallin1client.tour.finish.";
	private static final int TEXT_WIDTH = 320;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 8;
	private static final int TITLE_Y = 24;
	private static final int TEXT_TOP = 48;
	private static final int LINE_HEIGHT = 11;

	private final List<String> lines = new ArrayList<String>();

	public TourFinishScreen() {
		super(new TranslatableComponent(KEY + "title"));
	}

	@Override
	protected void init() {
		int textWidth = Math.min(TEXT_WIDTH, this.width - 40);
		this.lines.clear();
		this.lines.addAll(MenuText.wrap(I18n.get(KEY + "text"), textWidth));
		this.lines.add("");
		this.lines.addAll(MenuText.wrap(I18n.get(KEY + "world_hint"), textWidth));

		int y = TEXT_TOP + this.lines.size() * LINE_HEIGHT + 16;
		int x = (this.width - 2 * BUTTON_WIDTH - GAP) / 2;
		this.addButton(new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.get(KEY + "keep_minecraft"), pressed -> keep(false)));
		this.addButton(new Button(x + BUTTON_WIDTH + GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.get(KEY + "keep_client"), pressed -> keep(true)));
	}

	private void keep(boolean client) {
		ClientDesign.setClient(client);
		// A fresh pause menu (or title screen) - built for the design that was just picked.
		this.minecraft.setScreen(this.minecraft.level != null ? new PauseScreen(true) : new TitleScreen());
	}

	/** Not even Escape closes it. */
	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		this.renderBackground();
		MenuText.centered(I18n.get(KEY + "title"), this.width / 2, TITLE_Y, 0xFFFFFF);
		int y = TEXT_TOP;
		for (String line : this.lines) {
			MenuText.centered(line, this.width / 2, y, 0xFFFFFF);
			y += LINE_HEIGHT;
		}
		super.render(mouseX, mouseY, partialTick);
	}
}
