package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
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
 * of them instead; the switches always stay in one column (own user request). More switches than
 * fit above "Back" scroll (see {@link ScrollPane}).
 */
public abstract class FeatureOptionsScreen extends ClientScreen {
	private static final int FULL_WIDTH = 210;
	/** Switch width while the color picker sits beside them - both together fit the default window's width. */
	private static final int NARROW_WIDTH = 150;
	private static final int BESIDE_GAP = 10;
	private static final int PICKER_GAP = 6;
	private static final int ROW_HEIGHT = 20;
	/** Free space below every row. */
	private static final int ROW_GAP = 4;
	private static final int FIRST_ROW_Y = 34;
	private static final int HINT_GAP = 2;
	private static final int BACK_BUTTON_ID = 0;
	private static final int LEFT_MOUSE_BUTTON = 0;
	private static final int ESCAPE_KEY = 1;
	/** LWJGL 2's `Keyboard.KEY_NONE`. */
	private static final int UNBOUND = 0;
	/** The game counts mouse buttons as keys from here up: left -100, right -99, ... */
	private static final int FIRST_MOUSE_BUTTON_CODE = -100;

	private final List<Option> options = new ArrayList<Option>();
	/** The options on screen right now, top to bottom. */
	private List<Option> shown = new ArrayList<Option>();
	/** The button of each of them - null for one that is a panel. */
	private final List<ButtonWidget> optionButtons = new ArrayList<ButtonWidget>();
	/** The top edge of each of them, counted from the top of the first. */
	private final List<Integer> optionTops = new ArrayList<Integer>();
	/** Where the column of options is and how wide. */
	private int optionsLeft;
	private int optionsWidth;
	private final ScrollPane pane = new ScrollPane(this::placeOptions);
	private IntSupplier colorGetter;
	private IntConsumer colorSetter;
	private String hintKey;
	/** The key binding whose button was clicked and that gets the next key or mouse button pressed. */
	private KeyBinding listeningFor;

	protected FeatureOptionsScreen(Screen parent, String titleKey) {
		super(parent, titleKey);
	}

	/** An on/off switch, shown as "Label: ON". */
	protected final Option addToggle(final String labelKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return add(new Option(() -> MenuText.onOff(labelKey, getter.getAsBoolean()), () -> setter.accept(!getter.getAsBoolean())));
	}

	/** A switch between two named choices, shown as "Label: Choice". */
	protected final Option addChoice(final String labelKey, final String onKey, final String offKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return add(new Option(
				() -> I18n.translate(labelKey) + ": " + I18n.translate(getter.getAsBoolean() ? onKey : offKey),
				() -> setter.accept(!getter.getAsBoolean())));
	}

	/** A button stepping through more than two values, shown as "Label: Value". `next` switches to the value after the current one. */
	protected final Option addCycle(final String labelKey, final Supplier<String> valueKey, final Runnable next) {
		return add(new Option(() -> I18n.translate(labelKey) + ": " + I18n.translate(valueKey.get()), next));
	}

	/** A button that opens another screen. */
	protected final Option addLink(final String labelKey, final Supplier<Screen> screen) {
		return add(new Option(() -> I18n.translate(labelKey), () -> this.client.setScreen(screen.get())));
	}

	/** A slider for a whole number. The translation of `labelKey` takes the value as its argument. */
	protected final Option addSlider(final String labelKey, final int min, final int max, final IntSupplier getter, final IntConsumer setter) {
		return add(new Option(null, null) {
			@Override
			ButtonWidget createButton(int x, int width) {
				return new IntSliderButton(0, x, 0, width, ROW_HEIGHT, labelKey, min, max, getter.getAsInt(), setter);
			}
		});
	}

	/** An area that draws itself instead of a button. */
	protected final Option addPanel(final OptionPanel panel) {
		return add(new Option(null, null) {
			@Override
			ButtonWidget createButton(int x, int width) {
				return null;
			}

			@Override
			OptionPanel panel() {
				return panel;
			}
		});
	}

	/** A field to type text into, shown in red while `valid` rejects what is in it. `hintKey` names what to enter while it is empty. */
	protected final Option addTextField(String hintKey, int maxLength, Supplier<String> getter, Consumer<String> setter, Predicate<String> valid) {
		return addPanel(new TextFieldPanel(hintKey, maxLength, getter, setter, valid));
	}

	/**
	 * A button showing the key a key binding is on; clicked, the next key or mouse button pressed
	 * becomes the new one (Escape: none) - the same as in the game's Controls screen, which lists the
	 * binding too. The translation of `labelKey` takes the key's name as its argument.
	 */
	protected final Option addKeyBinding(final String labelKey, final KeyBinding keyBinding) {
		return add(new Option(
				() -> {
					String keyName = GameOptions.getFormattedNameForKeyCode(keyBinding.getCode());
					return I18n.translate(labelKey, this.listeningFor == keyBinding ? "> " + keyName + " <" : keyName);
				},
				() -> this.listeningFor = keyBinding));
	}

	private Option add(Option option) {
		this.options.add(option);
		return option;
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

	private List<Option> visibleOptions() {
		List<Option> visible = new ArrayList<Option>();
		for (Option option : this.options) {
			if (option.visible == null || option.visible.getAsBoolean()) {
				visible.add(option);
			}
		}
		return visible;
	}

	@Override
	public void init() {
		this.listeningFor = null;
		this.shown = visibleOptions();
		boolean hasColor = this.colorGetter != null;
		int backY = this.height - 28;
		this.optionTops.clear();
		int switchesHeight = 0;
		for (Option option : this.shown) {
			this.optionTops.add(switchesHeight);
			switchesHeight += option.height() + ROW_GAP;
		}
		// Does the picker still fit between the last switch and the "Back" button?
		boolean pickerBeside = hasColor
				&& FIRST_ROW_Y + switchesHeight + PICKER_GAP + ColorPickerPanel.totalHeight() > backY - PICKER_GAP;

		int buttonWidth = pickerBeside ? NARROW_WIDTH : FULL_WIDTH;
		int contentWidth = pickerBeside ? NARROW_WIDTH + BESIDE_GAP + ColorPickerPanel.totalWidth() : FULL_WIDTH;
		int left = (this.width - contentWidth) / 2;
		this.optionsLeft = left;
		this.optionsWidth = buttonWidth;
		this.optionButtons.clear();
		for (Option option : this.shown) {
			this.optionButtons.add(option.createButton(left, buttonWidth));
		}

		if (hasColor) {
			int pickerX = pickerBeside ? left + NARROW_WIDTH + BESIDE_GAP : (this.width - ColorPickerPanel.totalWidth()) / 2;
			int pickerY = pickerBeside ? FIRST_ROW_Y : FIRST_ROW_Y + switchesHeight + PICKER_GAP;
			final IntConsumer setter = this.colorSetter;
			this.colorPicker = new ColorPickerPanel(this.textRenderer, pickerX, pickerY, this.colorGetter.getAsInt(), argb -> {
				setter.accept(argb);
				ClientConfig.get().save();
			});
		}

		int contentHeight = switchesHeight - ROW_GAP;
		if (this.hintKey != null) {
			contentHeight = switchesHeight + HINT_GAP + this.textRenderer.fontHeight;
		}
		this.pane.layout(FIRST_ROW_Y, backY - PICKER_GAP, left + buttonWidth + ScrollPane.SCROLLBAR_GAP, contentHeight);

		this.buttons.add(new ButtonWidget(BACK_BUTTON_ID, (this.width - FULL_WIDTH) / 2, backY, FULL_WIDTH, ROW_HEIGHT, I18n.translate("gui.back")));
	}

	private void placeOptions() {
		for (int index = 0; index < this.optionButtons.size(); index++) {
			ButtonWidget button = this.optionButtons.get(index);
			if (button != null) {
				button.y = optionY(index);
			}
		}
	}

	/** Where the top edge of an option is on screen right now. */
	private int optionY(int index) {
		return FIRST_ROW_Y + this.optionTops.get(index) - this.pane.offset();
	}

	/** The height of all options on screen, including the free space below the last one. */
	private int optionsHeight() {
		int last = this.shown.size() - 1;
		return last < 0 ? 0 : this.optionTops.get(last) + this.shown.get(last).height() + ROW_GAP;
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK_BUTTON_ID) {
			back();
		}
	}

	private void optionClicked(Option option, ButtonWidget button) {
		// A slider has no click action - it follows the mouse by itself.
		if (option.onClick == null) {
			return;
		}
		option.onClick.run();
		ClientConfig.get().save();
		if (visibleOptions().equals(this.shown)) {
			button.message = option.label.get();
		} else {
			// The click switched something that other options depend on - lay the screen out anew.
			this.buttons.clear();
			init();
		}
	}

	@Override
	protected void keyPressed(char character, int code) {
		if (this.listeningFor != null) {
			bindListeningTo(code == ESCAPE_KEY ? UNBOUND : code);
			return;
		}
		if (code != ESCAPE_KEY) {
			for (Option option : this.shown) {
				OptionPanel panel = option.panel();
				if (panel != null && panel.keyPressed(character, code)) {
					return;
				}
			}
		}
		super.keyPressed(character, code);
	}

	@Override
	public void tick() {
		super.tick();
		for (Option option : this.shown) {
			OptionPanel panel = option.panel();
			if (panel != null) {
				panel.tick();
			}
		}
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		this.pane.handleWheel();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (this.listeningFor != null) {
			bindListeningTo(FIRST_MOUSE_BUTTON_CODE + button);
			return;
		}
		for (Option option : this.shown) {
			OptionPanel panel = option.panel();
			if (panel != null) {
				panel.unfocus();
			}
		}
		if (button == LEFT_MOUSE_BUTTON && this.pane.contains(mouseY)) {
			if (this.pane.mouseClicked(mouseX, mouseY)) {
				return;
			}
			for (int index = 0; index < this.optionButtons.size(); index++) {
				ButtonWidget optionButton = this.optionButtons.get(index);
				OptionPanel panel = this.shown.get(index).panel();
				if (panel != null) {
					if (panel.mouseClicked(this.optionsLeft, optionY(index), this.optionsWidth, mouseX, mouseY)) {
						// The click was not on the color picker - its text fields give up the keyboard focus.
						if (this.colorPicker != null) {
							this.colorPicker.mouseClicked(mouseX, mouseY, button);
						}
						return;
					}
				} else if (optionButton.isMouseOver(this.client, mouseX, mouseY)) {
					optionButton.playDownSound(this.client.getSoundManager());
					optionClicked(this.shown.get(index), optionButton);
					return;
				}
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long timeSinceClick) {
		this.pane.mouseDragged(mouseY);
		for (int index = 0; index < this.shown.size(); index++) {
			OptionPanel panel = this.shown.get(index).panel();
			if (panel != null) {
				panel.mouseDragged(this.optionsLeft, optionY(index), this.optionsWidth, mouseX, mouseY);
			}
		}
		super.mouseDragged(mouseX, mouseY, button, timeSinceClick);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		this.pane.mouseReleased();
		for (int index = 0; index < this.shown.size(); index++) {
			OptionPanel panel = this.shown.get(index).panel();
			if (panel != null) {
				panel.mouseReleased();
			} else {
				// Lets a slider go.
				this.optionButtons.get(index).mouseReleased(mouseX, mouseY);
			}
		}
		super.mouseReleased(mouseX, mouseY, button);
	}

	private void bindListeningTo(int code) {
		// Writes options.txt as well.
		this.client.options.setKeyBindingCode(this.listeningFor, code);
		KeyBinding.updateKeysByCode();
		this.listeningFor = null;
		for (int index = 0; index < this.optionButtons.size(); index++) {
			Option option = this.shown.get(index);
			if (option.label != null) {
				this.optionButtons.get(index).message = option.label.get();
			}
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);

		// A button scrolled half out of view must not light up under a cursor that is on the title or on "Back".
		int hoverY = this.pane.contains(mouseY) ? mouseY : -1;
		this.pane.beginClip(this.client);
		for (int index = 0; index < this.shown.size(); index++) {
			OptionPanel panel = this.shown.get(index).panel();
			if (panel != null) {
				panel.render(this.optionsLeft, optionY(index), this.optionsWidth);
			} else {
				this.optionButtons.get(index).render(this.client, mouseX, hoverY);
			}
		}
		if (this.hintKey != null) {
			int hintY = FIRST_ROW_Y + optionsHeight() + HINT_GAP - this.pane.offset();
			this.drawCenteredString(this.textRenderer, I18n.translate(this.hintKey), this.width / 2, hintY, 0xA0A0A0);
		}
		this.pane.endClip();
		this.pane.renderScrollbar();
	}

	/** One row of the screen. */
	protected static class Option {
		final Supplier<String> label;
		final Runnable onClick;
		BooleanSupplier visible;

		Option(Supplier<String> label, Runnable onClick) {
			this.label = label;
			this.onClick = onClick;
		}

		/** Shows the option only while the condition holds - for one that only makes sense with another one switched on. */
		public Option onlyIf(BooleanSupplier condition) {
			this.visible = condition;
			return this;
		}

		ButtonWidget createButton(int x, int width) {
			return new ButtonWidget(0, x, 0, width, ROW_HEIGHT, this.label.get());
		}

		/** Set for an option that is an area drawing itself instead of a button. */
		OptionPanel panel() {
			return null;
		}

		int height() {
			OptionPanel panel = panel();
			return panel != null ? panel.height() : ROW_HEIGHT;
		}
	}
}
