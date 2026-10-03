package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;

/**
 * A feature's own options screen: its switches one below the other, a color picker if the feature
 * has a color, "Back" at the bottom. A subclass only lists what there is to set (in its
 * constructor) - layout, clicks and saving happen here.
 *
 * <p>The color picker goes below the switches. Where that doesn't fit - four switches plus the
 * picker are taller than this version's screen at the default window size - it moves to the right
 * of them instead; the switches always stay in one column (own user request).
 */
public abstract class FeatureOptionsScreen extends ClientScreen {
	private static final int FULL_WIDTH = 210;
	/** Switch width while the color picker sits beside them - both together fit the default window's width. */
	private static final int NARROW_WIDTH = 150;
	private static final int BESIDE_GAP = 10;
	private static final int PICKER_GAP = 6;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int FIRST_ROW_Y = 34;
	private static final int BACK_BUTTON_ID = 0;
	/** An option's button gets this plus its index in {@link #options}. */
	private static final int FIRST_OPTION_ID = 100;
	private static final int ESCAPE_KEY = 1;
	/** LWJGL 2's `Keyboard.KEY_NONE`. */
	private static final int UNBOUND = 0;
	/** The game counts mouse buttons as keys from here up: left -100, right -99, ... */
	private static final int FIRST_MOUSE_BUTTON_CODE = -100;

	private final List<Option> options = new ArrayList<Option>();
	private IntSupplier colorGetter;
	private IntConsumer colorSetter;
	private String hintKey;
	private int hintY;
	/** The key binding whose button was clicked and that gets the next key or mouse button pressed. */
	private KeyBinding listeningFor;

	protected FeatureOptionsScreen(Screen parent, String titleKey) {
		super(parent, titleKey);
	}

	/** An on/off switch, shown as "Label: ON". */
	protected final void addToggle(final String labelKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		this.options.add(new Option(() -> MenuText.onOff(labelKey, getter.getAsBoolean()), () -> setter.accept(!getter.getAsBoolean())));
	}

	/** A switch between two named choices, shown as "Label: Choice". */
	protected final void addChoice(final String labelKey, final String onKey, final String offKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		this.options.add(new Option(
				() -> I18n.translate(labelKey) + ": " + I18n.translate(getter.getAsBoolean() ? onKey : offKey),
				() -> setter.accept(!getter.getAsBoolean())));
	}

	/** A button that opens another screen. */
	protected final void addLink(final String labelKey, final Supplier<Screen> screen) {
		this.options.add(new Option(() -> I18n.translate(labelKey), () -> this.client.setScreen(screen.get())));
	}

	/** A slider for a whole number. The translation of `labelKey` takes the value as its argument. */
	protected final void addSlider(final String labelKey, final int min, final int max, final IntSupplier getter, final IntConsumer setter) {
		this.options.add(new Option(null, null) {
			@Override
			ButtonWidget createButton(int id, int x, int y, int width) {
				return new IntSliderButton(id, x, y, width, ROW_HEIGHT, labelKey, min, max, getter.getAsInt(), setter);
			}
		});
	}

	/**
	 * A button showing the key a key binding is on; clicked, the next key or mouse button pressed
	 * becomes the new one (Escape: none) - the same as in the game's Controls screen, which lists the
	 * binding too. The translation of `labelKey` takes the key's name as its argument.
	 */
	protected final void addKeyBinding(final String labelKey, final KeyBinding keyBinding) {
		this.options.add(new Option(
				() -> {
					String keyName = GameOptions.getFormattedNameForKeyCode(keyBinding.getCode());
					return I18n.translate(labelKey, this.listeningFor == keyBinding ? "> " + keyName + " <" : keyName);
				},
				() -> this.listeningFor = keyBinding));
	}

	/** Gives the screen a color picker for the feature's one color. */
	protected final void setColor(IntSupplier getter, IntConsumer setter) {
		this.colorGetter = getter;
		this.colorSetter = setter;
	}

	/** A line of explanation below the switches. */
	protected final void setHint(String hintKey) {
		this.hintKey = hintKey;
	}

	@Override
	public void init() {
		this.listeningFor = null;
		boolean hasColor = this.colorGetter != null;
		int backY = this.height - 28;
		int switchesHeight = this.options.size() * ROW_SPACING;
		// Does the picker still fit between the last switch and the "Back" button?
		boolean pickerBeside = hasColor
				&& FIRST_ROW_Y + switchesHeight + PICKER_GAP + ColorPickerPanel.totalHeight() > backY - PICKER_GAP;

		int buttonWidth = pickerBeside ? NARROW_WIDTH : FULL_WIDTH;
		int contentWidth = pickerBeside ? NARROW_WIDTH + BESIDE_GAP + ColorPickerPanel.totalWidth() : FULL_WIDTH;
		int left = (this.width - contentWidth) / 2;
		int y = FIRST_ROW_Y;
		for (int index = 0; index < this.options.size(); index++) {
			this.buttons.add(this.options.get(index).createButton(FIRST_OPTION_ID + index, left, y, buttonWidth));
			y += ROW_SPACING;
		}
		this.hintY = y + 2;

		if (hasColor) {
			int pickerX = pickerBeside ? left + NARROW_WIDTH + BESIDE_GAP : (this.width - ColorPickerPanel.totalWidth()) / 2;
			int pickerY = pickerBeside ? FIRST_ROW_Y : y + PICKER_GAP;
			final IntConsumer setter = this.colorSetter;
			this.colorPicker = new ColorPickerPanel(this.textRenderer, pickerX, pickerY, this.colorGetter.getAsInt(), argb -> {
				setter.accept(argb);
				ClientConfig.get().save();
			});
		}

		this.buttons.add(new ButtonWidget(BACK_BUTTON_ID, (this.width - FULL_WIDTH) / 2, backY, FULL_WIDTH, ROW_HEIGHT, I18n.translate("gui.back")));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK_BUTTON_ID) {
			back();
			return;
		}
		int index = button.id - FIRST_OPTION_ID;
		if (index >= 0 && index < this.options.size()) {
			Option option = this.options.get(index);
			// A slider has neither - it follows the mouse by itself.
			if (option.onClick != null) {
				option.onClick.run();
				ClientConfig.get().save();
				button.message = option.label.get();
			}
		}
	}

	@Override
	protected void keyPressed(char character, int code) {
		if (this.listeningFor != null) {
			bindListeningTo(code == ESCAPE_KEY ? UNBOUND : code);
			return;
		}
		super.keyPressed(character, code);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (this.listeningFor != null) {
			bindListeningTo(FIRST_MOUSE_BUTTON_CODE + button);
			return;
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	private void bindListeningTo(int code) {
		// Writes options.txt as well.
		this.client.options.setKeyBindingCode(this.listeningFor, code);
		KeyBinding.updateKeysByCode();
		this.listeningFor = null;
		for (ButtonWidget button : this.buttons) {
			int index = button.id - FIRST_OPTION_ID;
			if (index >= 0 && index < this.options.size() && this.options.get(index).label != null) {
				button.message = this.options.get(index).label.get();
			}
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);
		if (this.hintKey != null) {
			this.drawCenteredString(this.textRenderer, I18n.translate(this.hintKey), this.width / 2, this.hintY, 0xA0A0A0);
		}
	}

	private static class Option {
		final Supplier<String> label;
		final Runnable onClick;

		Option(Supplier<String> label, Runnable onClick) {
			this.label = label;
			this.onClick = onClick;
		}

		ButtonWidget createButton(int id, int x, int y, int width) {
			return new ButtonWidget(id, x, y, width, ROW_HEIGHT, this.label.get());
		}
	}
}
