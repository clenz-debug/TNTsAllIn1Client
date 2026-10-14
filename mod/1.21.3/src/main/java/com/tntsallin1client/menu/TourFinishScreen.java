package com.tntsallin1client.menu;

import com.tntsallin1client.compat.EssentialCompat;
import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game tour's last screen (own user request): which design to keep, now that the tour
 * showed both - asked only here at the end. Also the reminder that the tour's world can simply be
 * deleted again. Can't be closed with Escape, the choice is the point of it. While Essential is
 * installed there's no choice (the design switch is locked), just "Done".
 */
public class TourFinishScreen extends Screen {
	private static final int TEXT_WIDTH = 320;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 8;
	private static final int TEXT_TOP = 48;

	private final List<FormattedCharSequence> lines = new ArrayList<>();

	public TourFinishScreen() {
		super(Component.translatable("gui.tntsallin1client.tour.finish.title"));
	}

	@Override
	protected void init() {
		int textWidth = Math.min(TEXT_WIDTH, this.width - 40);
		this.lines.clear();
		boolean choice = !EssentialCompat.isLoaded();
		this.lines.addAll(this.font.split(Component.translatable(choice ? "gui.tntsallin1client.tour.finish.text" : "gui.tntsallin1client.tour.finish.text_essential"), textWidth));
		this.lines.add(FormattedCharSequence.EMPTY);
		this.lines.addAll(this.font.split(Component.translatable("gui.tntsallin1client.tour.finish.world_hint"), textWidth));

		int y = TEXT_TOP + this.lines.size() * (this.font.lineHeight + 2) + 16;
		if (choice) {
			int x = (this.width - 2 * BUTTON_WIDTH - GAP) / 2;
			this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.tour.finish.keep_minecraft"), button -> keep(false))
					.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
			this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.tour.finish.keep_client"), button -> keep(true))
					.bounds(x + BUTTON_WIDTH + GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		} else {
			this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> back())
					.bounds((this.width - BUTTON_WIDTH) / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		}
	}

	private void keep(boolean client) {
		ClientDesign.setClient(client);
		back();
	}

	/** A fresh pause menu (or title screen) - built for the design that was just picked. */
	private void back() {
		this.minecraft.setScreen(this.minecraft.level != null ? new PauseScreen(true) : new TitleScreen());
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		MenuText.centered(graphics, this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
		int y = TEXT_TOP;
		for (FormattedCharSequence line : this.lines) {
			graphics.drawString(this.font, line, (this.width - this.font.width(line)) / 2, y, ThemedUi.active() ? ThemedUi.textColor(0xFFFFFFFF) : 0xFFFFFFFF, true);
			y += this.font.lineHeight + 2;
		}
	}
}
