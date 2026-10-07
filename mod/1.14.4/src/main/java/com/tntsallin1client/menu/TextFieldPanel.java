package com.tntsallin1client.menu;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;

/**
 * A text field as a row of a {@link FeatureOptionsScreen}: click to type, every change goes straight
 * to the setter and into the config. While empty and not being typed into, it shows a hint naming
 * what to enter; text the `valid` check rejects is shown in red.
 *
 * <p>The field takes whatever is typed - a setter for something other than text (a number) only
 * takes over what it can read and leaves the setting alone otherwise.
 */
final class TextFieldPanel implements OptionPanel {
	private static final int HEIGHT = 20;
	private static final int TEXT_COLOR = 0xE0E0E0;
	private static final int INVALID_COLOR = 0xFF5555;
	private static final int HINT_COLOR = 0x707070;

	private final Supplier<String> hint;
	private final int maxLength;
	private final Supplier<String> getter;
	private final Consumer<String> setter;
	private final Predicate<String> valid;
	/** Created once the row's width is known. */
	private EditBox field;
	private int fieldWidth;
	/** Whether the field has the keyboard focus from the start, without a click. */
	private boolean focusedAtFirst;
	/** Set for a field that can only be typed into while something else is set a certain way. */
	private BooleanSupplier editable;

	TextFieldPanel(final String hintKey, int maxLength, Supplier<String> getter, Consumer<String> setter, Predicate<String> valid) {
		this(() -> I18n.get(hintKey), maxLength, getter, setter, valid);
	}

	/** For a hint that is not a fixed text. */
	TextFieldPanel(Supplier<String> hint, int maxLength, Supplier<String> getter, Consumer<String> setter, Predicate<String> valid) {
		this.hint = hint;
		this.maxLength = maxLength;
		this.getter = getter;
		this.setter = setter;
		this.valid = valid;
	}

	/** Lets the player type right away - for the one field a screen is opened for. */
	TextFieldPanel focused() {
		this.focusedAtFirst = true;
		return this;
	}

	/** Locks the field while the condition does not hold: it keeps showing its text, grayed out, and takes no clicks. */
	TextFieldPanel editableIf(BooleanSupplier condition) {
		this.editable = condition;
		return this;
	}

	private boolean isEditable() {
		return this.editable == null || this.editable.getAsBoolean();
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	private EditBox fieldAt(int x, int y, int width) {
		if (this.field == null || this.fieldWidth != width) {
			boolean focused = this.field != null ? this.field.isFocused() : this.focusedAtFirst;
			this.focusedAtFirst = false;
			// The field's frame lies one pixel outside the box given here.
			this.field = new EditBox(Minecraft.getInstance().font, x + 1, y + 1, width - 2, HEIGHT - 2, "");
			this.field.setMaxLength(this.maxLength);
			this.field.setValue(this.getter.get());
			this.field.setFocus(focused);
			this.fieldWidth = width;
		}
		this.field.x = x + 1;
		this.field.y = y + 1;
		return this.field;
	}

	@Override
	public void render(int x, int y, int width, int mouseX, int mouseY) {
		EditBox field = fieldAt(x, y, width);
		field.setEditable(isEditable());
		field.setTextColor(this.valid.test(field.getValue()) ? TEXT_COLOR : INVALID_COLOR);
		field.render(mouseX, mouseY, 0.0F);
		if (field.getValue().isEmpty() && !field.isFocused()) {
			Font font = Minecraft.getInstance().font;
			MenuText.text(this.hint.get(), field.x + 4, field.y + (HEIGHT - 2 - font.lineHeight) / 2, HINT_COLOR);
		}
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		if (!isEditable() || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + HEIGHT) {
			return false;
		}
		EditBox field = fieldAt(x, y, width);
		field.setFocus(true);
		// Puts the cursor where the click was.
		field.mouseClicked(mouseX, mouseY, 0);
		return true;
	}

	@Override
	public void unfocus() {
		if (this.field != null) {
			this.field.setFocus(false);
		}
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (this.field == null || !this.field.isFocused()) {
			return false;
		}
		String before = this.field.getValue();
		this.field.keyPressed(key, scanCode, modifiers);
		takeOver(before);
		return true;
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		if (this.field == null || !this.field.isFocused()) {
			return false;
		}
		String before = this.field.getValue();
		this.field.charTyped(character, modifiers);
		takeOver(before);
		return true;
	}

	private void takeOver(String before) {
		if (!this.field.getValue().equals(before)) {
			this.setter.accept(this.field.getValue());
			ClientConfig.get().save();
		}
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
