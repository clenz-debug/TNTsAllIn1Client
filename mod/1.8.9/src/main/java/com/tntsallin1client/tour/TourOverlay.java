package com.tntsallin1client.tour;

import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.TitleScreenDesign;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;

/**
 * Draws the in-game tour ({@link InGameTour}) on top of the current screen: everything around the
 * highlighted part dimmed, a frame around it, and the explanation box next to it in the launcher's
 * theme colors - the same look as the launcher's own tour and the Fabric versions'. Remembers where
 * the box and its buttons ended up, for {@link InGameTour}'s click handling.
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
	/** The game's own style codes. */
	private static final String BOLD = "§l";
	private static final String UNDERLINED = "§n";

	/** Last frame's layout, in GUI coordinates - null where nothing was drawn. */
	static TourRect box;
	static TourRect nextButton;
	static TourRect endLink;
	static TourRect target;

	private TourOverlay() {
	}

	private enum Placement {
		/** Next to the highlighted part (or centered without one). */
		AROUND_TARGET,
		/** Small, in the top left corner - for hints that don't block anything. */
		CORNER
	}

	/** Called right after the game has drawn the screen on display. */
	public static void renderScreen(Screen screen) {
		clear();
		TourStep step = InGameTour.current();
		// The design switch animation runs on its own - the next step's target only exists once it's done.
		if (step == null || screen == null || TitleScreenDesign.isRunning()) {
			return;
		}
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int width = screen.width;
		int height = screen.height;
		// Above whatever the screen drew, tooltips and items included.
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, 0.0F, 400.0F);
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();

		if (!step.screen.test(screen)) {
			// Somewhere the step didn't expect (shouldn't happen) - leave the screen usable, keep "End tour" at hand.
			drawBox(font, width, height, step, null, Placement.CORNER, true);
		} else {
			TourRect resolved = step.target.resolve(screen);
			if (step.kind == TourStep.Kind.WAIT) {
				if (resolved != null) {
					outline(resolved.inflate(TARGET_PADDING), ClientTheme.get().text);
				}
				drawBox(font, width, height, step, null, Placement.CORNER, true);
			} else {
				target = resolved != null ? resolved.inflate(TARGET_PADDING) : null;
				dimAround(width, height, target);
				drawBox(font, width, height, step, target, Placement.AROUND_TARGET, true);
			}
		}
		GlStateManager.enableDepthTest();
		GlStateManager.popMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Steps without a screen (in the world, waiting for the pause menu) - a hint in the corner of the HUD. */
	public static void renderHud(MinecraftClient client) {
		if (client.currentScreen != null) {
			return;
		}
		clear();
		TourStep step = InGameTour.current();
		if (step == null || !step.screen.test(null)) {
			return;
		}
		Window window = new Window(client);
		drawBox(client.textRenderer, window.getWidth(), window.getHeight(), step, null, Placement.CORNER, false);
		// Nothing on the HUD can be clicked.
		clear();
	}

	private static void clear() {
		box = null;
		nextButton = null;
		endLink = null;
		target = null;
	}

	private static void outline(TourRect rect, int color) {
		DrawableHelper.fill(rect.x, rect.y, rect.right(), rect.y + 1, color);
		DrawableHelper.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), color);
		DrawableHelper.fill(rect.x, rect.y + 1, rect.x + 1, rect.bottom() - 1, color);
		DrawableHelper.fill(rect.right() - 1, rect.y + 1, rect.right(), rect.bottom() - 1, color);
	}

	private static void dimAround(int width, int height, TourRect hole) {
		if (hole == null) {
			DrawableHelper.fill(0, 0, width, height, DIM);
			return;
		}
		int top = Math.max(hole.y, 0);
		int bottom = Math.min(hole.bottom(), height);
		DrawableHelper.fill(0, 0, width, top, DIM);
		DrawableHelper.fill(0, bottom, width, height, DIM);
		DrawableHelper.fill(0, top, Math.max(hole.x, 0), bottom, DIM);
		DrawableHelper.fill(Math.min(hole.right(), width), top, width, bottom, DIM);
		outline(hole, ClientTheme.get().text);
	}

	private static void drawBox(TextRenderer font, int screenWidth, int screenHeight, TourStep step, TourRect around, Placement placement, boolean withLinks) {
		ClientTheme theme = ClientTheme.get();
		int boxWidth = Math.min(placement == Placement.CORNER ? CORNER_BOX_WIDTH : BOX_WIDTH, screenWidth - 2 * MARGIN);
		int inner = boxWidth - 2 * PADDING;
		int lineHeight = font.fontHeight + 1;

		String counter = I18n.translate(KEY + "counter", InGameTour.stepNumber(), InGameTour.stepCount());
		List<String> title = font.wrapLines(BOLD + I18n.translate(KEY + step.id + ".title"), inner);
		List<String> text = font.wrapLines(I18n.translate(KEY + step.id + ".text"), inner);
		List<String> hint = step.kind == TourStep.Kind.ACTION
				? font.wrapLines(I18n.translate(KEY + "hint_click"), inner)
				: Collections.<String>emptyList();
		boolean next = withLinks && step.hasNextButton();

		int boxHeight = PADDING + lineHeight + 3 + title.size() * lineHeight + 3 + text.size() * lineHeight
				+ (hint.isEmpty() ? 0 : 4 + hint.size() * lineHeight)
				+ (withLinks ? 6 + BUTTON_HEIGHT : 0) + PADDING - 1;
		int[] position = placement == Placement.CORNER
				? new int[] {MARGIN, MARGIN}
				: place(around, boxWidth, boxHeight, screenWidth, screenHeight);
		int x = position[0];
		int y = position[1];

		box = new TourRect(x, y, boxWidth, boxHeight);
		DrawableHelper.fill(x, y, x + boxWidth, y + boxHeight, theme.background2);
		outline(box, theme.accent4);

		int textX = x + PADDING;
		int cursor = y + PADDING;
		font.draw(counter, textX, cursor, ClientTheme.withAlpha(theme.text, 0.65F));
		cursor += lineHeight + 3;
		for (String line : title) {
			font.draw(line, textX, cursor, theme.text);
			cursor += lineHeight;
		}
		cursor += 3;
		for (String line : text) {
			font.draw(line, textX, cursor, theme.text);
			cursor += lineHeight;
		}
		if (!hint.isEmpty()) {
			cursor += 4;
			for (String line : hint) {
				font.draw(line, textX, cursor, HINT_COLOR);
				cursor += lineHeight;
			}
		}
		if (!withLinks) {
			return;
		}

		int buttonsY = cursor + 6;
		String end = I18n.translate(KEY + "end");
		int linkY = buttonsY + (BUTTON_HEIGHT - font.fontHeight) / 2 + 1;
		font.draw(UNDERLINED + end, textX, linkY, ClientTheme.withAlpha(theme.text, 0.8F));
		endLink = new TourRect(textX - 2, buttonsY, font.getStringWidth(end) + 4, BUTTON_HEIGHT);

		if (next) {
			String label = I18n.translate(KEY + "next");
			int buttonWidth = font.getStringWidth(label) + 16;
			int buttonX = x + boxWidth - PADDING - buttonWidth;
			nextButton = new TourRect(buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT);
			DrawableHelper.fill(buttonX, buttonsY, buttonX + buttonWidth, buttonsY + BUTTON_HEIGHT, theme.accent3);
			outline(nextButton, theme.accent4);
			font.draw(label, buttonX + 8, linkY, theme.text);
		}
	}

	/** Below the highlighted part if it fits, else above, else beside it, else the bottom right corner. */
	private static int[] place(TourRect around, int width, int height, int screenWidth, int screenHeight) {
		int maxX = Math.max(MARGIN, screenWidth - width - MARGIN);
		int maxY = Math.max(MARGIN, screenHeight - height - MARGIN);
		if (around == null) {
			return new int[] {(screenWidth - width) / 2, Math.max(MARGIN, (screenHeight - height) / 2)};
		}
		int centeredX = clamp(around.x + around.width / 2 - width / 2, MARGIN, maxX);
		if (around.bottom() + GAP + height <= screenHeight - MARGIN) {
			return new int[] {centeredX, around.bottom() + GAP};
		}
		if (around.y - GAP - height >= MARGIN) {
			return new int[] {centeredX, around.y - GAP - height};
		}
		if (around.right() + GAP + width <= screenWidth - MARGIN) {
			return new int[] {around.right() + GAP, clamp(around.y, MARGIN, maxY)};
		}
		if (around.x - GAP - width >= MARGIN) {
			return new int[] {around.x - GAP - width, clamp(around.y, MARGIN, maxY)};
		}
		return new int[] {maxX, maxY};
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
