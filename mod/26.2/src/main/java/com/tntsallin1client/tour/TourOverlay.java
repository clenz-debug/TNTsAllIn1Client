package com.tntsallin1client.tour;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.TitleScreenDesign;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws the in-game tour ({@link InGameTour}) on top of the current screen: everything around the
 * highlighted part dimmed, a frame around it, and the explanation box next to it in the launcher's
 * theme colors - the same look as the launcher's own tour. Remembers where the box and its buttons
 * ended up, for {@link InGameTour}'s click handling.
 */
public final class TourOverlay {
	private static final int DIM = 0xB0000000;
	private static final int HINT_COLOR = 0xFFFFCC66;
	private static final int TARGET_PADDING = 3;
	private static final int MARGIN = 8;
	private static final int GAP = 6;
	private static final int PADDING = 8;
	private static final int BOX_WIDTH = 250;
	private static final int CORNER_BOX_WIDTH = 190;
	private static final int BUTTON_HEIGHT = 14;
	private static final String KEY = "gui.tntsallin1client.tour.";

	/** Last frame's layout, in GUI coordinates - null where nothing was drawn. */
	static @Nullable TourRect box;
	static @Nullable TourRect nextButton;
	static @Nullable TourRect endLink;
	static @Nullable TourRect target;

	private TourOverlay() {
	}

	private enum Placement {
		/** Next to the highlighted part (or centered without one). */
		AROUND_TARGET,
		/** Small, in the top left corner - for hints that don't block anything. */
		CORNER
	}

	static void renderScreen(Screen screen, GuiGraphicsExtractor graphics) {
		clear();
		TourStep step = InGameTour.current();
		// The design switch animation runs on its own - the next step's target only exists once it's done.
		if (step == null || TitleScreenDesign.isRunning()) return;
		Font font = Minecraft.getInstance().font;
		int width = screen.width;
		int height = screen.height;
		graphics.nextStratum();

		if (!step.screen().test(screen)) {
			// Somewhere the step didn't expect (shouldn't happen) - leave the screen usable, keep "Tour beenden" at hand.
			drawBox(graphics, font, width, height, step, null, Placement.CORNER, true);
			return;
		}

		TourRect resolved = step.target().resolve(screen);
		if (step.kind() == TourStep.Kind.WAIT) {
			if (resolved != null) {
				TourRect frame = resolved.inflate(TARGET_PADDING);
				graphics.outline(frame.x(), frame.y(), frame.width(), frame.height(), ClientTheme.get().text);
			}
			drawBox(graphics, font, width, height, step, null, Placement.CORNER, true);
			return;
		}

		target = resolved != null ? resolved.inflate(TARGET_PADDING) : null;
		dimAround(graphics, width, height, target);
		drawBox(graphics, font, width, height, step, target, Placement.AROUND_TARGET, true);
	}

	/** Steps without a screen (in the world, waiting for the pause menu) - a hint in the corner of the HUD. */
	static void renderHud(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.screen() != null) return;
		clear();
		TourStep step = InGameTour.current();
		if (step == null || !step.screen().test(null)) return;
		drawBox(graphics, minecraft.font, graphics.guiWidth(), graphics.guiHeight(), step, null, Placement.CORNER, false);
		// Nothing on the HUD can be clicked.
		clear();
	}

	private static void clear() {
		box = null;
		nextButton = null;
		endLink = null;
		target = null;
	}

	private static void dimAround(GuiGraphicsExtractor graphics, int width, int height, @Nullable TourRect hole) {
		if (hole == null) {
			graphics.fill(0, 0, width, height, DIM);
			return;
		}
		int top = Math.max(hole.y(), 0);
		int bottom = Math.min(hole.bottom(), height);
		graphics.fill(0, 0, width, top, DIM);
		graphics.fill(0, bottom, width, height, DIM);
		graphics.fill(0, top, Math.max(hole.x(), 0), bottom, DIM);
		graphics.fill(Math.min(hole.right(), width), top, width, bottom, DIM);
		graphics.outline(hole.x(), hole.y(), hole.width(), hole.height(), ClientTheme.get().text);
	}

	private static void drawBox(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, TourStep step,
			@Nullable TourRect around, Placement placement, boolean withLinks) {
		ClientTheme theme = ClientTheme.get();
		int boxWidth = Math.min(placement == Placement.CORNER ? CORNER_BOX_WIDTH : BOX_WIDTH, screenWidth - 2 * MARGIN);
		int inner = boxWidth - 2 * PADDING;
		int lineHeight = font.lineHeight + 1;

		Component counter = Component.translatable(KEY + "counter", InGameTour.stepNumber(), InGameTour.stepCount());
		List<FormattedCharSequence> title = font.split(Component.translatable(KEY + step.id() + ".title").withStyle(ChatFormatting.BOLD), inner);
		List<FormattedCharSequence> text = font.split(Component.translatable(KEY + step.id() + ".text"), inner);
		List<FormattedCharSequence> hint = step.kind() == TourStep.Kind.ACTION
				? font.split(Component.translatable(KEY + "hint_click"), inner)
				: List.of();
		boolean links = withLinks;
		boolean next = withLinks && step.hasNextButton();

		int boxHeight = PADDING + lineHeight + 3 + title.size() * lineHeight + 3 + text.size() * lineHeight
				+ (hint.isEmpty() ? 0 : 4 + hint.size() * lineHeight)
				+ (links ? 6 + BUTTON_HEIGHT : 0) + PADDING - 1;
		int[] position = placement == Placement.CORNER
				? new int[] {MARGIN, MARGIN}
				: place(around, boxWidth, boxHeight, screenWidth, screenHeight);
		int x = position[0];
		int y = position[1];

		graphics.fill(x, y, x + boxWidth, y + boxHeight, theme.background2);
		graphics.outline(x, y, boxWidth, boxHeight, theme.accent4);
		box = new TourRect(x, y, boxWidth, boxHeight);

		int textX = x + PADDING;
		int cursor = y + PADDING;
		graphics.text(font, counter, textX, cursor, ClientTheme.withAlpha(theme.text, 0.65f), false);
		cursor += lineHeight + 3;
		for (FormattedCharSequence line : title) {
			graphics.text(font, line, textX, cursor, theme.text, false);
			cursor += lineHeight;
		}
		cursor += 3;
		for (FormattedCharSequence line : text) {
			graphics.text(font, line, textX, cursor, theme.text, false);
			cursor += lineHeight;
		}
		if (!hint.isEmpty()) {
			cursor += 4;
			for (FormattedCharSequence line : hint) {
				graphics.text(font, line, textX, cursor, HINT_COLOR, false);
				cursor += lineHeight;
			}
		}
		if (!links) return;

		int buttonsY = cursor + 6;
		Component end = Component.translatable(KEY + "end").withStyle(ChatFormatting.UNDERLINE);
		int endWidth = font.width(end);
		int linkY = buttonsY + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
		graphics.text(font, end, textX, linkY, ClientTheme.withAlpha(theme.text, 0.8f), false);
		endLink = new TourRect(textX - 2, buttonsY, endWidth + 4, BUTTON_HEIGHT);

		if (next) {
			Component label = Component.translatable(KEY + "next");
			int buttonWidth = font.width(label) + 16;
			int buttonX = x + boxWidth - PADDING - buttonWidth;
			graphics.fill(buttonX, buttonsY, buttonX + buttonWidth, buttonsY + BUTTON_HEIGHT, theme.accent3);
			graphics.outline(buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT, theme.accent4);
			graphics.text(font, label, buttonX + 8, linkY, theme.text, false);
			nextButton = new TourRect(buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT);
		}
	}

	/** Below the highlighted part if it fits, else above, else beside it, else the bottom right corner. */
	private static int[] place(@Nullable TourRect around, int width, int height, int screenWidth, int screenHeight) {
		int maxX = Math.max(MARGIN, screenWidth - width - MARGIN);
		int maxY = Math.max(MARGIN, screenHeight - height - MARGIN);
		if (around == null) {
			return new int[] {(screenWidth - width) / 2, Math.max(MARGIN, (screenHeight - height) / 2)};
		}
		int centeredX = clamp(around.x() + around.width() / 2 - width / 2, MARGIN, maxX);
		if (around.bottom() + GAP + height <= screenHeight - MARGIN) {
			return new int[] {centeredX, around.bottom() + GAP};
		}
		if (around.y() - GAP - height >= MARGIN) {
			return new int[] {centeredX, around.y() - GAP - height};
		}
		if (around.right() + GAP + width <= screenWidth - MARGIN) {
			return new int[] {around.right() + GAP, clamp(around.y(), MARGIN, maxY)};
		}
		if (around.x() - GAP - width >= MARGIN) {
			return new int[] {around.x() - GAP - width, clamp(around.y(), MARGIN, maxY)};
		}
		return new int[] {maxX, maxY};
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
