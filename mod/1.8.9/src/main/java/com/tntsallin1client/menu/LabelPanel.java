package com.tntsallin1client.menu;

import java.util.function.Supplier;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

/**
 * A line of text as a row of a {@link FeatureOptionsScreen}: gray and centered on the column of rows,
 * or - as the heading of the row below it - white and at the column's left edge.
 */
final class LabelPanel implements OptionPanel {
	private static final int HEIGHT = 12;
	private static final int TEXT_Y = 3;
	private static final int COLOR = 0xAAAAAA;
	private static final int HEADING_COLOR = 0xFFFFFF;

	private final Supplier<String> text;
	private final boolean heading;

	LabelPanel(Supplier<String> text) {
		this(text, false);
	}

	LabelPanel(Supplier<String> text, boolean heading) {
		this.text = text;
		this.heading = heading;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	@Override
	public void render(int x, int y, int width) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		String text = this.text.get();
		if (this.heading) {
			textRenderer.drawWithShadow(text, x, y + TEXT_Y, HEADING_COLOR);
		} else {
			textRenderer.drawWithShadow(text, x + (width - textRenderer.getStringWidth(text)) / 2, y + TEXT_Y, COLOR);
		}
	}
}
