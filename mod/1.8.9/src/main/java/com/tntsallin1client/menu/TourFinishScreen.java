package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.design.ClientDesign;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * The in-game tour's last screen, as in the Fabric versions: which design to keep, now that the
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
	private static final int KEEP_MINECRAFT_BUTTON_ID = 0;
	private static final int KEEP_CLIENT_BUTTON_ID = 1;

	private final List<String> lines = new ArrayList<String>();

	@Override
	public void init() {
		int textWidth = Math.min(TEXT_WIDTH, this.width - 40);
		this.lines.clear();
		this.lines.addAll(MenuText.wrap(I18n.translate(KEY + "text"), textWidth));
		this.lines.add("");
		this.lines.addAll(MenuText.wrap(I18n.translate(KEY + "world_hint"), textWidth));

		int y = TEXT_TOP + this.lines.size() * LINE_HEIGHT + 16;
		int x = (this.width - 2 * BUTTON_WIDTH - GAP) / 2;
		this.buttons.add(new ButtonWidget(KEEP_MINECRAFT_BUTTON_ID, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.translate(KEY + "keep_minecraft")));
		this.buttons.add(new ButtonWidget(KEEP_CLIENT_BUTTON_ID, x + BUTTON_WIDTH + GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.translate(KEY + "keep_client")));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		ClientDesign.setClient(button.id == KEEP_CLIENT_BUTTON_ID);
		// A fresh pause menu (or title screen) - built for the design that was just picked.
		this.client.setScreen(this.client.world != null ? new GameMenuScreen() : new TitleScreen());
	}

	@Override
	protected void keyPressed(char character, int code) {
		// Not even Escape closes it.
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		this.renderBackground();
		MenuText.centered(I18n.translate(KEY + "title"), this.width / 2, TITLE_Y, 0xFFFFFF);
		int y = TEXT_TOP;
		for (String line : this.lines) {
			MenuText.centered(line, this.width / 2, y, 0xFFFFFF);
			y += LINE_HEIGHT;
		}
		super.render(mouseX, mouseY, tickDelta);
	}
}
