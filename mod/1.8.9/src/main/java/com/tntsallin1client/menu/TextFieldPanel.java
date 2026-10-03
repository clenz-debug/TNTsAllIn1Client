package com.tntsallin1client.menu;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * A text field as a row of a {@link FeatureOptionsScreen}: click to type, every change goes straight
 * to the setter and into the config. While empty and not being typed into, it shows a hint naming
 * what to enter; text the `valid` check rejects is shown in red.
 */
final class TextFieldPanel implements OptionPanel {
	private static final int HEIGHT = 20;
	private static final int TEXT_COLOR = 0xE0E0E0;
	private static final int INVALID_COLOR = 0xFF5555;
	private static final int HINT_COLOR = 0x707070;

	private final String hintKey;
	private final int maxLength;
	private final Supplier<String> getter;
	private final Consumer<String> setter;
	private final Predicate<String> valid;
	/** Created once the row's width is known - the game's text field can't change its size afterwards. */
	private TextFieldWidget field;
	private int fieldWidth;

	TextFieldPanel(String hintKey, int maxLength, Supplier<String> getter, Consumer<String> setter, Predicate<String> valid) {
		this.hintKey = hintKey;
		this.maxLength = maxLength;
		this.getter = getter;
		this.setter = setter;
		this.valid = valid;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	private TextFieldWidget fieldAt(int x, int y, int width) {
		if (this.field == null || this.fieldWidth != width) {
			boolean focused = this.field != null && this.field.isFocused();
			// The field's frame lies one pixel outside the box given here.
			this.field = new TextFieldWidget(0, MinecraftClient.getInstance().textRenderer, x + 1, y + 1, width - 2, HEIGHT - 2);
			this.field.setMaxLength(this.maxLength);
			this.field.setText(this.getter.get());
			this.field.setFocused(focused);
			this.fieldWidth = width;
		}
		this.field.x = x + 1;
		this.field.y = y + 1;
		return this.field;
	}

	@Override
	public void render(int x, int y, int width) {
		TextFieldWidget field = fieldAt(x, y, width);
		field.setEditableColor(this.valid.test(field.getText()) ? TEXT_COLOR : INVALID_COLOR);
		field.render();
		if (field.getText().isEmpty() && !field.isFocused()) {
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			textRenderer.drawWithShadow(I18n.translate(this.hintKey), field.x + 4, field.y + (HEIGHT - 2 - textRenderer.fontHeight) / 2, HINT_COLOR);
		}
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + HEIGHT) {
			return false;
		}
		TextFieldWidget field = fieldAt(x, y, width);
		// Focuses the field and puts the cursor where the click was.
		field.mouseClicked(mouseX, mouseY, 0);
		return true;
	}

	@Override
	public void unfocus() {
		if (this.field != null) {
			this.field.setFocused(false);
		}
	}

	@Override
	public boolean keyPressed(char character, int code) {
		if (this.field == null || !this.field.isFocused()) {
			return false;
		}
		String before = this.field.getText();
		this.field.keyPressed(character, code);
		if (!this.field.getText().equals(before)) {
			this.setter.accept(this.field.getText());
			ClientConfig.get().save();
		}
		return true;
	}

	/** The field is made anew, with the current text, the next time it is drawn. */
	@Override
	public void reload() {
		this.field = null;
	}

	@Override
	public void tick() {
		if (this.field != null) {
			this.field.tick();
		}
	}
}
