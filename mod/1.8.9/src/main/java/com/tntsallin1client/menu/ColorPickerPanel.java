package com.tntsallin1client.menu;

import java.awt.Color;
import java.util.function.IntConsumer;

import com.google.common.base.Predicate;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

/**
 * Color picker shared by every feature with a color to choose: a saturation/brightness square, a
 * hue slider next to it, and red/green/blue/hex text fields - the same picker as in the Fabric
 * versions of the mod. One difference in layout: the fields sit to the right of the square instead
 * of below it. Stacked, the picker is taller than this version's screen at the default window size
 * (240 scaled pixels), which would push a screen's "Back" button off the bottom.
 *
 * <p>Not a widget the screen could manage - this version's screens only know buttons. The owning
 * screen passes its render, mouse, key and tick events on, which {@link ClientScreen} does for
 * whatever picker a screen sets.
 */
public class ColorPickerPanel extends DrawableHelper {
	private static final int SQUARE_SIZE = 100;
	private static final int GRADIENT_STEP = 2;
	private static final int HUE_SLIDER_WIDTH = 16;
	private static final int HUE_SLIDER_GAP = 10;
	private static final int MARKER_HALF_SIZE = 3;
	private static final int FIELD_COLUMN_GAP = 12;
	private static final int FIELD_WIDTH = 52;
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_SPACING = 26;
	private static final int LABEL_GAP = 6;
	/** Room for the longest field label ("Green"/"Grün") before the swatch. */
	private static final int LABEL_WIDTH = 34;
	private static final int SWATCH_SIZE = 20;
	private static final int LEFT_MOUSE_BUTTON = 0;

	private int x;
	private int y;
	private final TextRenderer textRenderer;
	private final TextFieldWidget redField;
	private final TextFieldWidget greenField;
	private final TextFieldWidget blueField;
	private final TextFieldWidget hexField;
	private final IntConsumer onChange;

	private float hue;
	private float saturation;
	private float value;
	private boolean draggingSquare;
	private boolean draggingHue;

	public ColorPickerPanel(TextRenderer textRenderer, int x, int y, int initialArgb, IntConsumer onChange) {
		this.x = x;
		this.y = y;
		this.textRenderer = textRenderer;
		this.onChange = onChange;
		syncHsvFrom(initialArgb);

		int fieldX = fieldX();
		this.redField = channelField(0, fieldX, y);
		this.greenField = channelField(1, fieldX, y + FIELD_SPACING);
		this.blueField = channelField(2, fieldX, y + FIELD_SPACING * 2);
		this.hexField = new TextFieldWidget(3, textRenderer, fieldX, y + FIELD_SPACING * 3, FIELD_WIDTH, FIELD_HEIGHT);
		this.hexField.setMaxLength(6);
		this.hexField.setTextPredicate(matching("[0-9a-fA-F]{0,6}"));

		showColor(initialArgb);
	}

	private TextFieldWidget channelField(int id, int fieldX, int fieldY) {
		TextFieldWidget field = new TextFieldWidget(id, this.textRenderer, fieldX, fieldY, FIELD_WIDTH, FIELD_HEIGHT);
		field.setMaxLength(3);
		field.setTextPredicate(matching("[0-9]{0,3}"));
		return field;
	}

	private static Predicate<String> matching(final String pattern) {
		return new Predicate<String>() {
			@Override
			public boolean apply(String text) {
				return text.matches(pattern);
			}
		};
	}

	/** Horizontal space this panel occupies, so the owning screen can center it. */
	public static int totalWidth() {
		return SQUARE_SIZE + HUE_SLIDER_GAP + HUE_SLIDER_WIDTH + FIELD_COLUMN_GAP + FIELD_WIDTH + LABEL_GAP + LABEL_WIDTH + SWATCH_SIZE;
	}

	/** Vertical space this panel occupies, so the owning screen can lay out whatever comes after it. */
	public static int totalHeight() {
		return SQUARE_SIZE;
	}

	private int hueX() {
		return this.x + SQUARE_SIZE + HUE_SLIDER_GAP;
	}

	private int fieldX() {
		return hueX() + HUE_SLIDER_WIDTH + FIELD_COLUMN_GAP;
	}

	/** Puts the whole picker somewhere else - for one that scrolls with the screen's other rows. */
	public void moveTo(int x, int y) {
		this.x = x;
		this.y = y;
		int fieldX = fieldX();
		TextFieldWidget[] fields = fields();
		for (int index = 0; index < fields.length; index++) {
			fields[index].x = fieldX;
			fields[index].y = y + FIELD_SPACING * index;
		}
	}

	/** Whether one of the text fields is being typed into. */
	public boolean hasFocus() {
		for (TextFieldWidget field : fields()) {
			if (field.isFocused()) {
				return true;
			}
		}
		return false;
	}

	public void unfocus() {
		for (TextFieldWidget field : fields()) {
			field.setFocused(false);
		}
	}

	private TextFieldWidget[] fields() {
		return new TextFieldWidget[] {this.redField, this.greenField, this.blueField, this.hexField};
	}

	public void render(int labelColor) {
		for (int column = 0; column < SQUARE_SIZE; column += GRADIENT_STEP) {
			float columnSaturation = (float) column / (SQUARE_SIZE - 1);
			int top = Color.HSBtoRGB(this.hue, columnSaturation, 1.0F);
			this.fillGradient(this.x + column, this.y, this.x + column + GRADIENT_STEP, this.y + SQUARE_SIZE, top, 0xFF000000);
		}
		int squareMarkerX = this.x + Math.round(this.saturation * (SQUARE_SIZE - 1));
		int squareMarkerY = this.y + Math.round((1.0F - this.value) * (SQUARE_SIZE - 1));
		drawMarker(squareMarkerX, squareMarkerY);

		int hueX = hueX();
		for (int row = 0; row < SQUARE_SIZE; row += GRADIENT_STEP) {
			float rowHue = (float) row / (SQUARE_SIZE - 1);
			fill(hueX, this.y + row, hueX + HUE_SLIDER_WIDTH, this.y + row + GRADIENT_STEP, Color.HSBtoRGB(rowHue, 1.0F, 1.0F));
		}
		int hueMarkerY = this.y + Math.round(this.hue * (SQUARE_SIZE - 1));
		fill(hueX - 1, hueMarkerY - 1, hueX + HUE_SLIDER_WIDTH + 1, hueMarkerY + 2, 0xFF000000);
		fill(hueX, hueMarkerY, hueX + HUE_SLIDER_WIDTH, hueMarkerY + 1, 0xFFFFFFFF);

		for (TextFieldWidget field : fields()) {
			field.render();
		}
		int labelX = fieldX() + FIELD_WIDTH + LABEL_GAP;
		drawLabel("gui.tntsallin1client.color_picker.red", labelX, this.redField.y, labelColor);
		drawLabel("gui.tntsallin1client.color_picker.green", labelX, this.greenField.y, labelColor);
		drawLabel("gui.tntsallin1client.color_picker.blue", labelX, this.blueField.y, labelColor);
		drawLabel("gui.tntsallin1client.color_picker.hex", labelX, this.hexField.y, labelColor);
		ColorPickerHelper.drawSwatch(labelX + LABEL_WIDTH, this.hexField.y, SWATCH_SIZE, currentColor());
	}

	private void drawLabel(String key, int labelX, int fieldY, int color) {
		MenuText.text(I18n.translate(key), labelX, fieldY + 6, color);
	}

	private static void drawMarker(int centerX, int centerY) {
		int half = MARKER_HALF_SIZE;
		fill(centerX - half - 1, centerY - half - 1, centerX + half + 1, centerY + half + 1, 0xFF000000);
		fill(centerX - half, centerY - half, centerX + half, centerY + half, 0xFFFFFFFF);
	}

	public void mouseClicked(int mouseX, int mouseY, int button) {
		// Each field takes or drops the keyboard focus depending on whether the click hit it.
		for (TextFieldWidget field : fields()) {
			field.mouseClicked(mouseX, mouseY, button);
		}
		if (button != LEFT_MOUSE_BUTTON) {
			return;
		}
		if (isInSquare(mouseX, mouseY)) {
			this.draggingSquare = true;
			updateFromSquare(mouseX, mouseY);
		} else if (isInHueSlider(mouseX, mouseY)) {
			this.draggingHue = true;
			updateFromHueSlider(mouseY);
		}
	}

	public void mouseDragged(int mouseX, int mouseY) {
		if (this.draggingSquare) {
			updateFromSquare(mouseX, mouseY);
		} else if (this.draggingHue) {
			updateFromHueSlider(mouseY);
		}
	}

	public void mouseReleased() {
		this.draggingSquare = false;
		this.draggingHue = false;
	}

	public void keyPressed(char character, int code) {
		for (TextFieldWidget field : fields()) {
			if (!field.isFocused()) {
				continue;
			}
			String before = field.getText();
			field.keyPressed(character, code);
			if (!field.getText().equals(before)) {
				if (field == this.hexField) {
					onHexEdited();
				} else {
					onChannelEdited();
				}
			}
			return;
		}
	}

	/** Keeps the text cursors blinking. */
	public void tick() {
		for (TextFieldWidget field : fields()) {
			field.tick();
		}
	}

	private boolean isInSquare(int mouseX, int mouseY) {
		return mouseX >= this.x && mouseX < this.x + SQUARE_SIZE && mouseY >= this.y && mouseY < this.y + SQUARE_SIZE;
	}

	private boolean isInHueSlider(int mouseX, int mouseY) {
		int hueX = hueX();
		return mouseX >= hueX && mouseX < hueX + HUE_SLIDER_WIDTH && mouseY >= this.y && mouseY < this.y + SQUARE_SIZE;
	}

	private void updateFromSquare(int mouseX, int mouseY) {
		this.saturation = MathHelper.clamp((float) (mouseX - this.x) / (SQUARE_SIZE - 1), 0.0F, 1.0F);
		this.value = 1.0F - MathHelper.clamp((float) (mouseY - this.y) / (SQUARE_SIZE - 1), 0.0F, 1.0F);
		pushColor(currentColor());
	}

	private void updateFromHueSlider(int mouseY) {
		this.hue = MathHelper.clamp((float) (mouseY - this.y) / (SQUARE_SIZE - 1), 0.0F, 1.0F);
		pushColor(currentColor());
	}

	private void onChannelEdited() {
		int argb = 0xFF000000
				| (parseChannel(this.redField.getText()) << 16)
				| (parseChannel(this.greenField.getText()) << 8)
				| parseChannel(this.blueField.getText());
		syncHsvFrom(argb);
		// Only the other kind of field is rewritten - replacing the text being typed (e.g. "300"
		// with the clamped "255") would move the cursor under the player's fingers.
		setIfChanged(this.hexField, ColorPickerHelper.toHexRgb(argb));
		this.onChange.accept(argb);
	}

	private void onHexEdited() {
		Integer parsed = ColorPickerHelper.parseHexRgbToArgb(this.hexField.getText());
		if (parsed == null) {
			return;
		}
		syncHsvFrom(parsed);
		showChannels(parsed);
		this.onChange.accept(parsed);
	}

	private int currentColor() {
		return Color.HSBtoRGB(this.hue, this.saturation, this.value);
	}

	private void syncHsvFrom(int argb) {
		float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
		this.hue = hsb[0];
		this.saturation = hsb[1];
		this.value = hsb[2];
	}

	/** A color picked with the square or the hue slider: into every field, and out to the owner. */
	private void pushColor(int argb) {
		showColor(argb);
		this.onChange.accept(argb);
	}

	private void showColor(int argb) {
		setIfChanged(this.hexField, ColorPickerHelper.toHexRgb(argb));
		showChannels(argb);
	}

	private void showChannels(int argb) {
		setIfChanged(this.redField, String.valueOf((argb >> 16) & 0xFF));
		setIfChanged(this.greenField, String.valueOf((argb >> 8) & 0xFF));
		setIfChanged(this.blueField, String.valueOf(argb & 0xFF));
	}

	private static void setIfChanged(TextFieldWidget field, String newValue) {
		if (!field.getText().equals(newValue)) {
			field.setText(newValue);
		}
	}

	private static int parseChannel(String value) {
		try {
			return MathHelper.clamp(Integer.parseInt(value.isEmpty() ? "0" : value), 0, 255);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
