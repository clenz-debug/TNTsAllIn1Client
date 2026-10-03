package com.tntsallin1client.menu;

import java.util.function.Supplier;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

/** A line of gray text as a row of a {@link FeatureOptionsScreen}, centered on the column of rows. */
final class LabelPanel implements OptionPanel {
	private static final int HEIGHT = 12;
	private static final int TEXT_Y = 3;
	private static final int COLOR = 0xAAAAAA;

	private final Supplier<String> text;

	LabelPanel(Supplier<String> text) {
		this.text = text;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	@Override
	public void render(int x, int y, int width) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		String text = this.text.get();
		textRenderer.drawWithShadow(text, x + (width - textRenderer.getStringWidth(text)) / 2, y + TEXT_Y, COLOR);
	}
}
