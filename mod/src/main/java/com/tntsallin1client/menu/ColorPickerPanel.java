package com.tntsallin1client.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Slider/square-based color picker (hue/saturation/value square + a hue
 * slider, plus RGB number fields and the existing hex field), replacing the
 * hex-only input the crosshair/hitbox screens used before - typing a hex
 * code was the only way to change a color, no visual picking at all. Now
 * used by {@link SingleColorOptionsScreen}, reached from {@link ColorMenuScreen}.
 * Not a full copy of the referenced MS Paint dialog
 * (which is HSL - a big hue/saturation square plus a separate luminosity
 * slider): built on HSV instead via {@code Mth.hsvToArgb}, already provided
 * by vanilla, and {@code java.awt.Color.RGBtoHSB} for the reverse direction
 * (already an accepted dependency in this project - see
 * {@code ScreenshotChatLink}'s clipboard code) - same idea (2D field + 1D
 * slider instead of only typing numbers), simpler math, one fewer thing to
 * get wrong by hand.
 *
 * <p>Not an {@link AbstractWidget} itself - it owns and positions a handful
 * of real widgets (the RGB/hex {@link EditBox}es, added to the owning screen
 * via the {@code registerWidget} callback passed in) plus draws/handles input
 * for the square and hue slider directly, the same way {@link HudEditorScreen}
 * handles its own drag boxes outside the normal widget system. The owning
 * screen forwards {@code render}/{@code mouseClicked}/{@code mouseDragged}/
 * {@code mouseReleased} to this class.
 *
 * <p>{@code suppressResponders} exists because {@code EditBox#setValue}
 * always re-invokes that box's own responder (confirmed in the decompiled
 * source - it is not a "silent" setter) - without the guard, programmatically
 * pushing the recomputed hex/RGB text after a square/slider drag would
 * immediately re-parse and re-derive hue/saturation/value from that same
 * text, which is harmless in principle (idempotent) but pointless extra work
 * every dragged pixel, and risks visible jitter from the RGB round-trip's
 * integer rounding.
 */
public class ColorPickerPanel {
	private static final int SQUARE_SIZE = 100;
	private static final int GRADIENT_STEP = 2;
	private static final int HUE_SLIDER_WIDTH = 16;
	private static final int HUE_SLIDER_GAP = 10;
	private static final int MARKER_HALF_SIZE = 3;
	private static final int SWATCH_SIZE = 20;
	private static final int SWATCH_GAP = 4;

	private final int x;
	private final int y;
	private final Font font;
	private final EditBox hexField;
	private final EditBox redField;
	private final EditBox greenField;
	private final EditBox blueField;
	private final IntConsumer onChange;

	private float hue;
	private float saturation;
	private float value;
	private boolean draggingSquare;
	private boolean draggingHue;
	private boolean suppressResponders;

	public ColorPickerPanel(Font font, int x, int y, int width, int initialArgb,
			Consumer<AbstractWidget> registerWidget, IntConsumer onChange) {
		this.x = x;
		this.y = y;
		this.font = font;
		this.onChange = onChange;
		syncHsvFrom(initialArgb);

		int fieldRowWidth = width - HUE_SLIDER_WIDTH - HUE_SLIDER_GAP;
		int fieldY = y + SQUARE_SIZE + 8;
		int labelWidth = 42;
		int fieldWidth = fieldRowWidth - labelWidth - 6;
		// The hex field + its swatch together take up exactly one "field column"
		// (same width as a single R/G/B box), so the "Hex" label lines up with
		// the Red/Green/Blue labels instead of sitting way out past the hue
		// slider where the old full-width hex field + misplaced swatch used to end.
		int hexFieldWidth = fieldWidth - SWATCH_SIZE - SWATCH_GAP;

		this.redField = new EditBox(font, x, fieldY, fieldWidth, 20, Component.translatable("gui.tntsallin1client.color_picker.red"));
		this.greenField = new EditBox(font, x, fieldY + 24, fieldWidth, 20, Component.translatable("gui.tntsallin1client.color_picker.green"));
		this.blueField = new EditBox(font, x, fieldY + 48, fieldWidth, 20, Component.translatable("gui.tntsallin1client.color_picker.blue"));
		this.hexField = new EditBox(font, x, fieldY + 72, hexFieldWidth, 20, Component.translatable("gui.tntsallin1client.color_picker.hex"));
		this.hexField.setMaxLength(6);
		this.hexField.setFilter(v -> v.matches("[0-9a-fA-F]{0,6}"));
		for (EditBox channelField : new EditBox[]{this.redField, this.greenField, this.blueField}) {
			channelField.setMaxLength(3);
			channelField.setFilter(v -> v.matches("[0-9]{0,3}"));
		}

		this.redField.setResponder(v -> onChannelEdited());
		this.greenField.setResponder(v -> onChannelEdited());
		this.blueField.setResponder(v -> onChannelEdited());
		this.hexField.setResponder(this::onHexEdited);

		pushColor(initialArgb);

		registerWidget.accept(this.redField);
		registerWidget.accept(this.greenField);
		registerWidget.accept(this.blueField);
		registerWidget.accept(this.hexField);
	}

	/** Total vertical space this panel occupies, so the owning screen can lay out whatever comes after it. */
	public static int totalHeight() {
		return SQUARE_SIZE + 8 + 24 * 3 + 24;
	}

	public void render(GuiGraphics guiGraphics, int labelColor) {
		for (int column = 0; column < SQUARE_SIZE; column += GRADIENT_STEP) {
			float columnSaturation = (float) column / (SQUARE_SIZE - 1);
			int top = Mth.hsvToArgb(this.hue, columnSaturation, 1.0F, 255);
			guiGraphics.fillGradient(this.x + column, this.y, this.x + column + GRADIENT_STEP, this.y + SQUARE_SIZE, top, 0xFF000000);
		}
		int squareMarkerX = this.x + Math.round(this.saturation * (SQUARE_SIZE - 1));
		int squareMarkerY = this.y + Math.round((1.0F - this.value) * (SQUARE_SIZE - 1));
		drawMarker(guiGraphics, squareMarkerX, squareMarkerY);

		int hueX = this.x + SQUARE_SIZE + HUE_SLIDER_GAP;
		for (int row = 0; row < SQUARE_SIZE; row += GRADIENT_STEP) {
			float rowHue = (float) row / (SQUARE_SIZE - 1);
			int color = Mth.hsvToArgb(rowHue, 1.0F, 1.0F, 255);
			guiGraphics.fill(hueX, this.y + row, hueX + HUE_SLIDER_WIDTH, this.y + row + GRADIENT_STEP, color);
		}
		int hueMarkerY = this.y + Math.round(this.hue * (SQUARE_SIZE - 1));
		guiGraphics.fill(hueX - 1, hueMarkerY - 1, hueX + HUE_SLIDER_WIDTH + 1, hueMarkerY + 2, 0xFF000000);
		guiGraphics.fill(hueX, hueMarkerY, hueX + HUE_SLIDER_WIDTH, hueMarkerY + 1, 0xFFFFFFFF);

		int labelX = this.x + this.redField.getWidth() + 6;
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.color_picker.red"), labelX, this.redField.getY() + 6, labelColor);
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.color_picker.green"), labelX, this.greenField.getY() + 6, labelColor);
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.color_picker.blue"), labelX, this.blueField.getY() + 6, labelColor);

		int swatchX = this.hexField.getX() + this.hexField.getWidth() + SWATCH_GAP;
		ColorPickerHelper.drawSwatch(guiGraphics, swatchX, this.hexField.getY(), SWATCH_SIZE, Mth.hsvToArgb(this.hue, this.saturation, this.value, 255));
		guiGraphics.drawString(this.font, Component.translatable("gui.tntsallin1client.color_picker.hex"), labelX, this.hexField.getY() + 6, labelColor);
	}

	public boolean mouseClicked(MouseButtonEvent event) {
		if (isInSquare(event.x(), event.y())) {
			this.draggingSquare = true;
			updateFromSquare(event.x(), event.y());
			return true;
		}
		if (isInHueSlider(event.x(), event.y())) {
			this.draggingHue = true;
			updateFromHueSlider(event.y());
			return true;
		}
		return false;
	}

	public boolean mouseDragged(MouseButtonEvent event) {
		if (this.draggingSquare) {
			updateFromSquare(event.x(), event.y());
			return true;
		}
		if (this.draggingHue) {
			updateFromHueSlider(event.y());
			return true;
		}
		return false;
	}

	public boolean mouseReleased() {
		boolean wasDragging = this.draggingSquare || this.draggingHue;
		this.draggingSquare = false;
		this.draggingHue = false;
		return wasDragging;
	}

	private boolean isInSquare(double mouseX, double mouseY) {
		return mouseX >= this.x && mouseX < this.x + SQUARE_SIZE && mouseY >= this.y && mouseY < this.y + SQUARE_SIZE;
	}

	private boolean isInHueSlider(double mouseX, double mouseY) {
		int hueX = this.x + SQUARE_SIZE + HUE_SLIDER_GAP;
		return mouseX >= hueX && mouseX < hueX + HUE_SLIDER_WIDTH && mouseY >= this.y && mouseY < this.y + SQUARE_SIZE;
	}

	private void updateFromSquare(double mouseX, double mouseY) {
		this.saturation = Mth.clamp((float) (mouseX - this.x) / (SQUARE_SIZE - 1), 0.0F, 1.0F);
		this.value = 1.0F - Mth.clamp((float) (mouseY - this.y) / (SQUARE_SIZE - 1), 0.0F, 1.0F);
		pushColor(Mth.hsvToArgb(this.hue, this.saturation, this.value, 255));
	}

	private void updateFromHueSlider(double mouseY) {
		this.hue = Mth.clamp((float) (mouseY - this.y) / (SQUARE_SIZE - 1), 0.0F, 1.0F);
		pushColor(Mth.hsvToArgb(this.hue, this.saturation, this.value, 255));
	}

	private void onChannelEdited() {
		if (this.suppressResponders) {
			return;
		}
		int r = parseChannel(this.redField.getValue());
		int g = parseChannel(this.greenField.getValue());
		int b = parseChannel(this.blueField.getValue());
		int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
		syncHsvFrom(argb);
		pushColor(argb);
	}

	private void onHexEdited(String hex) {
		if (this.suppressResponders) {
			return;
		}
		Integer parsed = ColorPickerHelper.parseHexRgbToArgb(hex);
		if (parsed == null) {
			return;
		}
		syncHsvFrom(parsed);
		pushColor(parsed);
	}

	private void syncHsvFrom(int argb) {
		float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
		this.hue = hsb[0];
		this.saturation = hsb[1];
		this.value = hsb[2];
	}

	private void pushColor(int argb) {
		this.suppressResponders = true;
		setIfChanged(this.hexField, ColorPickerHelper.toHexRgb(argb));
		setIfChanged(this.redField, String.valueOf((argb >> 16) & 0xFF));
		setIfChanged(this.greenField, String.valueOf((argb >> 8) & 0xFF));
		setIfChanged(this.blueField, String.valueOf(argb & 0xFF));
		this.suppressResponders = false;
		this.onChange.accept(argb);
	}

	private static void setIfChanged(EditBox field, String newValue) {
		if (!field.getValue().equals(newValue)) {
			field.setValue(newValue);
		}
	}

	private static int parseChannel(String value) {
		try {
			return Mth.clamp(Integer.parseInt(value.isEmpty() ? "0" : value), 0, 255);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static void drawMarker(GuiGraphics guiGraphics, int centerX, int centerY) {
		int half = MARKER_HALF_SIZE;
		guiGraphics.fill(centerX - half - 1, centerY - half - 1, centerX + half + 1, centerY + half + 1, 0xFF000000);
		guiGraphics.fill(centerX - half, centerY - half, centerX + half, centerY + half, 0xFFFFFFFF);
	}
}
