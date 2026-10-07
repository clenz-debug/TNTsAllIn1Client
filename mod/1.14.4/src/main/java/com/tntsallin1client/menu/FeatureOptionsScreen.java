package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

/**
 * A feature's own options screen: its switches one below the other, a color picker if the feature
 * has a color, "Back" at the bottom. A subclass only lists what there is to set (in its
 * constructor) - layout, clicks and saving happen here.
 *
 * <p>Everything is one column, as in the other versions: the rows, below them the color picker of
 * a feature with one color, then "Move / Resize HUD" where the feature has something on the HUD,
 * then "Back". A screen whose point is one action (create, delete) has that button down there too,
 * apart from the settings: above "Back" or to its left. What doesn't fit scrolls (see
 * {@link ScrollPane}) - the buttons at the end included. "Reset" stays put, right of the column and
 * level with its first row.
 *
 * <p>In the client design "Back", "Move / Resize" and "Reset" are a bar along the top instead
 * ({@link TopBar}); only a screen's action button stays below its settings.
 */
public abstract class FeatureOptionsScreen extends ClientScreen {
	private static final int FULL_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	/** Free space below every row. */
	private static final int ROW_GAP = 4;
	private static final int FIRST_ROW_Y = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int HINT_GAP = 2;
	/** Extra space between the settings and the buttons that follow them. */
	private static final int FOOTER_GAP = 6;
	private static final int RESET_BUTTON_WIDTH = 84;
	private static final int RESET_BUTTON_MARGIN = 6;
	/** The scrollbar's room right of the rows, which "Reset" keeps clear of. */
	private static final int SCROLLBAR_SPACE = 16;
	/** Shown instead of the word where a narrow window leaves no room for it. */
	private static final String SHORT_RESET_LABEL = "↺";
	private static final int LEFT_MOUSE_BUTTON = 0;

	private final List<Option> options = new ArrayList<Option>();
	/** The rows on screen right now, top to bottom: the options, then the color picker and the buttons at the end. */
	private List<Option> shown = new ArrayList<Option>();
	/** The options among them - what {@link #visibleOptions()} gave when the screen was laid out. */
	private List<Option> shownOptions = new ArrayList<Option>();
	/** The button of each of them - null for one that is a panel. */
	private final List<AbstractWidget> optionButtons = new ArrayList<AbstractWidget>();
	/** The top edge of each of them, counted from the top of the first. */
	private final List<Integer> optionTops = new ArrayList<Integer>();
	/** Where the column of options is and how wide. */
	private int optionsLeft;
	private int optionsWidth;
	private final ScrollPane pane = new ScrollPane(this::placeOptions);
	/** The feature's one color, as the row after all options - see {@link #setColor}. */
	private Option colorOption;
	private String hintKey;
	/** The hint's top edge, counted like {@link #optionTops}. */
	private int hintTop;
	/** Set while "Reset" shows {@link #SHORT_RESET_LABEL} and explains itself under the cursor. */
	private Button shortResetButton;
	/** Set for a screen with a "Reset" button. */
	private ConfigReset.Feature resetFeature;
	/** Set for a screen with a button of its own next to "Back" - see {@link #setAction}. */
	private String actionLabelKey;
	private Runnable action;
	private boolean actionBesideBack;
	/** The key binding whose button was clicked and that gets the next key or mouse button pressed. */
	private KeyMapping listeningFor;
	/** The slider the mouse button went down on - it follows the mouse until the button is let go. */
	private AbstractWidget draggedSlider;

	protected FeatureOptionsScreen(Screen parent, String titleKey) {
		super(parent, titleKey);
	}

	/** An on/off switch, shown as "Label: ON". */
	protected final Option addToggle(final String labelKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return add(new Option(() -> MenuText.onOff(labelKey, getter.getAsBoolean()), () -> setter.accept(!getter.getAsBoolean())));
	}

	/** An on/off switch for something that has a name instead of a translation key. */
	protected final Option addNamedToggle(final Supplier<String> name, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return add(new Option(
				() -> name.get() + ": " + I18n.get(getter.getAsBoolean() ? "options.on" : "options.off"),
				() -> setter.accept(!getter.getAsBoolean())));
	}

	/** A switch between two named choices, shown as "Label: Choice". */
	protected final Option addChoice(final String labelKey, final String onKey, final String offKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return add(new Option(
				() -> I18n.get(labelKey) + ": " + I18n.get(getter.getAsBoolean() ? onKey : offKey),
				() -> setter.accept(!getter.getAsBoolean())));
	}

	/** A switch between two named choices that shows nothing but the current one. */
	protected final Option addValueSwitch(final String onKey, final String offKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return add(new Option(() -> I18n.get(getter.getAsBoolean() ? onKey : offKey), () -> setter.accept(!getter.getAsBoolean())));
	}

	/** A button stepping through more than two values, shown as "Label: Value". `next` switches to the value after the current one. */
	protected final Option addCycle(final String labelKey, final Supplier<String> valueKey, final Runnable next) {
		return add(new Option(() -> I18n.get(labelKey) + ": " + I18n.get(valueKey.get()), next));
	}

	/** A button that opens another screen. */
	protected final Option addLink(final String labelKey, final Supplier<Screen> screen) {
		return add(new Option(() -> I18n.get(labelKey), () -> this.minecraft.setScreen(screen.get())));
	}

	/** Two on/off switches sharing a row, each half as wide. */
	protected final Option addTogglePair(String firstLabelKey, BooleanSupplier firstGetter, Consumer<Boolean> firstSetter,
			String secondLabelKey, BooleanSupplier secondGetter, Consumer<Boolean> secondSetter) {
		return addPanel(new SplitPanel(togglePanel(firstLabelKey, firstGetter, firstSetter), togglePanel(secondLabelKey, secondGetter, secondSetter)));
	}

	private static ButtonPanel togglePanel(final String labelKey, final BooleanSupplier getter, final Consumer<Boolean> setter) {
		return new ButtonPanel(() -> MenuText.onOff(labelKey, getter.getAsBoolean()), () -> setter.accept(!getter.getAsBoolean()));
	}

	/** A line of text between the rows - a heading for the ones below it, or something to read. */
	protected final Option addLabel(Supplier<String> text) {
		return addPanel(new LabelPanel(text));
	}

	/** The name of the row below it (a color picker), at the left edge of the column. */
	protected final Option addHeading(final String labelKey) {
		return addPanel(new LabelPanel(() -> I18n.get(labelKey), true));
	}

	/** A slider for a whole number. The translation of `labelKey` takes the value as its argument. */
	protected final Option addSlider(String labelKey, int min, int max, IntSupplier getter, IntConsumer setter) {
		return addSlider(labelKey, Integer.MAX_VALUE, min, max, getter, setter);
	}

	/** A slider no wider than `maxWidth`, at the left edge of the column. */
	protected final Option addSlider(final String labelKey, final int maxWidth, final int min, final int max, final IntSupplier getter, final IntConsumer setter) {
		return add(new Option(null, null) {
			@Override
			AbstractWidget createButton(int x, int width) {
				return new IntSliderButton(x, 0, Math.min(width, maxWidth), ROW_HEIGHT, labelKey, min, max, getter.getAsInt(), setter);
			}
		});
	}

	/** An area that draws itself instead of a button. */
	protected final Option addPanel(OptionPanel panel) {
		return add(panelOption(panel));
	}

	private static Option panelOption(final OptionPanel panel) {
		return new Option(null, null) {
			@Override
			AbstractWidget createButton(int x, int width) {
				return null;
			}

			@Override
			OptionPanel panel() {
				return panel;
			}
		};
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
	protected final Option addKeyBinding(final String labelKey, final KeyMapping keyBinding) {
		return add(new Option(
				() -> {
					String keyName = keyBinding.getTranslatedKeyMessage();
					return I18n.get(labelKey, this.listeningFor == keyBinding ? "> " + keyName + " <" : keyName);
				},
				() -> this.listeningFor = keyBinding));
	}

	private Option add(Option option) {
		this.options.add(option);
		return option;
	}

	/**
	 * A color picker as a row among the others - for a screen with more than one color, or with rows
	 * that belong below the picker. It scrolls with them.
	 */
	protected final Option addColor(IntSupplier getter, IntConsumer setter) {
		return addPanel(new ColorPanel(getter, setter));
	}

	/** Gives the screen a color picker for the feature's one color, placed after all rows. */
	protected final void setColor(IntSupplier getter, IntConsumer setter) {
		this.colorOption = panelOption(new ColorPanel(getter, setter));
	}

	/** A line of explanation below the options. */
	protected final void setHint(String hintKey) {
		this.hintKey = hintKey;
	}

	/** What the button at the bottom says - "Back", unless leaving the screen drops what was entered on it. */
	protected String backLabelKey() {
		return "gui.back";
	}

	/**
	 * Gives the screen a button for what it is there to do, kept apart from its settings: directly
	 * above "Back", or - `besideBack` - sharing the last row with it, the action on the left.
	 */
	protected final void setAction(String labelKey, Runnable action, boolean besideBack) {
		this.actionLabelKey = labelKey;
		this.action = action;
		this.actionBesideBack = besideBack;
	}

	/** Gives the screen a "Reset" button right of its rows that puts the feature's settings back to their defaults. */
	protected final void setResettable(ConfigReset.Feature feature) {
		this.resetFeature = feature;
	}

	/**
	 * Whether the screen belongs to a feature with something on the HUD: the feature's own screen
	 * says so itself, a screen opened from another options screen (a color, a shape) follows that one.
	 */
	private boolean offersHudEditor() {
		if (this.resetFeature != null) {
			return this.resetFeature.hasHudElement();
		}
		return this.parent instanceof FeatureOptionsScreen && ((FeatureOptionsScreen) this.parent).offersHudEditor();
	}

	/** Asks first. Either way the player is back on this screen afterwards. */
	private void askReset() {
		this.minecraft.setScreen(new ThemedConfirmScreen(confirmed -> {
			if (confirmed) {
				ConfigReset.reset(this.resetFeature);
				for (Option option : this.shown) {
					OptionPanel panel = option.panel();
					if (panel != null) {
						panel.reload();
					}
				}
			}
			this.minecraft.setScreen(this);
		}, I18n.get("gui.tntsallin1client.reset.confirm_title", I18n.get(this.resetFeature.labelKey)),
				I18n.get("gui.tntsallin1client.reset.confirm_message")));
	}

	private void openHudEditor() {
		this.minecraft.setScreen(new HudEditorScreen(this));
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

	/** The buttons that follow the settings, top to bottom. */
	private List<Option> footerOptions() {
		List<Option> footer = new ArrayList<Option>();
		if (TopBar.inUse()) {
			// "Move / Resize" and "Back" are in the top bar.
			if (this.action != null) {
				footer.add(new Option(() -> I18n.get(this.actionLabelKey), this.action));
			}
			return footer;
		}
		// "Move / Resize HUD" on the screens of features that have something on the HUD (own user
		// request, both the button and leaving it out elsewhere).
		if (offersHudEditor()) {
			footer.add(new Option(() -> I18n.get("gui.tntsallin1client.menu.hud_editor_button"), this::openHudEditor));
		}
		Supplier<String> backLabel = () -> I18n.get(backLabelKey());
		if (this.action != null && this.actionBesideBack) {
			footer.add(panelOption(new SplitPanel(
					new ButtonPanel(() -> I18n.get(this.actionLabelKey), this.action), new ButtonPanel(backLabel, this::back))));
			return footer;
		}
		if (this.action != null) {
			footer.add(new Option(() -> I18n.get(this.actionLabelKey), this.action));
		}
		footer.add(new Option(backLabel, this::back));
		return footer;
	}

	@Override
	protected void init() {
		this.listeningFor = null;
		this.draggedSlider = null;
		this.shownOptions = visibleOptions();
		this.shown = new ArrayList<Option>(this.shownOptions);
		if (this.colorOption != null) {
			this.shown.add(this.colorOption);
		}
		int settings = this.shown.size();
		this.shown.addAll(footerOptions());

		this.optionTops.clear();
		int y = 0;
		for (int index = 0; index < this.shown.size(); index++) {
			if (index == settings) {
				this.hintTop = y - ROW_GAP + HINT_GAP;
				if (this.hintKey != null) {
					y = this.hintTop + this.font.lineHeight + ROW_GAP;
				}
				y += FOOTER_GAP;
			}
			this.optionTops.add(y);
			y += this.shown.get(index).height() + ROW_GAP;
		}
		if (settings == this.shown.size()) {
			// Nothing follows the settings: the hint is the last thing in the column.
			this.hintTop = y - ROW_GAP + HINT_GAP;
			if (this.hintKey != null) {
				y = this.hintTop + this.font.lineHeight + ROW_GAP;
			}
		}
		int contentHeight = Math.max(0, y - ROW_GAP);

		int left = (this.width - FULL_WIDTH) / 2;
		this.optionsLeft = left;
		this.optionsWidth = FULL_WIDTH;
		this.optionButtons.clear();
		for (Option option : this.shown) {
			this.optionButtons.add(option.createButton(left, FULL_WIDTH));
		}

		// The scrollbar goes right of the widest row - a panel may be wider than the column of buttons.
		int rowsRight = left + FULL_WIDTH;
		for (Option option : this.shown) {
			OptionPanel panel = option.panel();
			if (panel != null) {
				rowsRight = Math.max(rowsRight, left + (FULL_WIDTH + panel.width(FULL_WIDTH)) / 2);
			}
		}
		this.pane.layout(FIRST_ROW_Y, this.height - BOTTOM_MARGIN, rowsRight + ScrollPane.SCROLLBAR_GAP, contentHeight);

		this.shortResetButton = null;
		boolean topBar = TopBar.inUse();
		boolean hudEditor = offersHudEditor();
		if (topBar) {
			this.addButton(TopBar.back(I18n.get(backLabelKey()), this::back));
			if (hudEditor) {
				this.addButton(TopBar.hudEditor(this.width, this::openHudEditor));
			}
		}
		if (this.resetFeature != null) {
			// In the top bar "Reset" sits below "Move / Resize" - level with the first rows, like in the
			// Minecraft design - or in its place, above them, where there is none.
			boolean besideRows = !topBar || hudEditor;
			int margin = topBar ? TopBar.MARGIN : RESET_BUTTON_MARGIN;
			int fullWidth = topBar ? TopBar.BUTTON_WIDTH : RESET_BUTTON_WIDTH;
			boolean fits = !besideRows || this.width - margin - fullWidth >= rowsRight + SCROLLBAR_SPACE;
			int resetWidth = fits ? fullWidth : ROW_HEIGHT;
			int resetY = !topBar ? FIRST_ROW_Y : hudEditor ? TopBar.SECOND_ROW_Y : TopBar.Y;
			Button reset = this.addButton(new Button(this.width - margin - resetWidth, resetY, resetWidth, ROW_HEIGHT,
					fits ? I18n.get("gui.tntsallin1client.reset.button") : SHORT_RESET_LABEL, pressed -> askReset()));
			if (!fits) {
				this.shortResetButton = reset;
			}
		}
	}

	private void placeOptions() {
		for (int index = 0; index < this.optionButtons.size(); index++) {
			AbstractWidget button = this.optionButtons.get(index);
			if (button != null) {
				button.y = optionY(index);
			}
		}
	}

	/** Where the top edge of an option is on screen right now. */
	private int optionY(int index) {
		return FIRST_ROW_Y + this.optionTops.get(index) - this.pane.offset();
	}

	private void optionClicked(Option option, AbstractWidget button) {
		option.onClick.run();
		ClientConfig.get().save();
		// The click may have led to another screen ("Back").
		if (this.minecraft.screen != this) {
			return;
		}
		if (visibleOptions().equals(this.shownOptions)) {
			button.setMessage(option.label.get());
		} else {
			// The click switched something that other options depend on - lay the screen out anew.
			rebuild();
		}
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (this.listeningFor != null) {
			bindListeningTo(key == GLFW.GLFW_KEY_ESCAPE ? InputConstants.UNKNOWN : InputConstants.getKey(key, scanCode));
			return true;
		}
		if (key != GLFW.GLFW_KEY_ESCAPE) {
			for (Option option : this.shown) {
				OptionPanel panel = option.panel();
				if (panel != null && panel.keyPressed(key, scanCode, modifiers)) {
					return true;
				}
			}
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		for (Option option : this.shown) {
			OptionPanel panel = option.panel();
			if (panel != null && panel.charTyped(character, modifiers)) {
				return true;
			}
		}
		return super.charTyped(character, modifiers);
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
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		this.pane.mouseScrolled(amount);
		return true;
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		int mouseX = (int) x;
		int mouseY = (int) y;
		if (this.listeningFor != null) {
			bindListeningTo(InputConstants.Type.MOUSE.getOrCreate(button));
			return true;
		}
		for (Option option : this.shown) {
			OptionPanel panel = option.panel();
			if (panel != null) {
				panel.unfocus();
			}
		}
		if (button == LEFT_MOUSE_BUTTON && this.pane.contains(mouseY)) {
			if (this.pane.mouseClicked(mouseX, mouseY)) {
				return true;
			}
			for (int index = 0; index < this.optionButtons.size(); index++) {
				AbstractWidget optionButton = this.optionButtons.get(index);
				Option option = this.shown.get(index);
				OptionPanel panel = option.panel();
				if (panel != null) {
					if (panel.mouseClicked(this.optionsLeft, optionY(index), this.optionsWidth, mouseX, mouseY)) {
						return true;
					}
				} else if (optionButton.isMouseOver(x, y)) {
					if (option.onClick == null) {
						// A slider: it plays its own sound and follows the mouse by itself from here.
						optionButton.mouseClicked(x, y, button);
						this.draggedSlider = optionButton;
					} else {
						optionButton.playDownSound(this.minecraft.getSoundManager());
						optionClicked(option, optionButton);
					}
					return true;
				}
			}
		}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
		int mouseX = (int) x;
		int mouseY = (int) y;
		this.pane.mouseDragged(mouseY);
		for (int index = 0; index < this.shown.size(); index++) {
			OptionPanel panel = this.shown.get(index).panel();
			if (panel != null) {
				panel.mouseDragged(this.optionsLeft, optionY(index), this.optionsWidth, mouseX, mouseY);
			}
		}
		if (this.draggedSlider != null) {
			this.draggedSlider.mouseDragged(x, y, button, deltaX, deltaY);
		}
		return super.mouseDragged(x, y, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		this.pane.mouseReleased();
		for (Option option : this.shown) {
			OptionPanel panel = option.panel();
			if (panel != null) {
				panel.mouseReleased();
			}
		}
		if (this.draggedSlider != null) {
			this.draggedSlider.mouseReleased(x, y, button);
			this.draggedSlider = null;
		}
		return super.mouseReleased(x, y, button);
	}

	private void bindListeningTo(InputConstants.Key key) {
		// Writes options.txt as well.
		this.minecraft.options.setKey(this.listeningFor, key);
		KeyMapping.resetMapping();
		this.listeningFor = null;
		for (int index = 0; index < this.optionButtons.size(); index++) {
			Option option = this.shown.get(index);
			if (option.label != null && this.optionButtons.get(index) != null) {
				this.optionButtons.get(index).setMessage(option.label.get());
			}
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		super.render(mouseX, mouseY, partialTick);

		// A button scrolled half out of view must not light up under a cursor that is on the title.
		int hoverY = this.pane.contains(mouseY) ? mouseY : -1;
		this.pane.beginClip(this.minecraft);
		for (int index = 0; index < this.shown.size(); index++) {
			OptionPanel panel = this.shown.get(index).panel();
			if (panel != null) {
				panel.render(this.optionsLeft, optionY(index), this.optionsWidth, mouseX, hoverY);
			} else {
				this.optionButtons.get(index).render(mouseX, hoverY, partialTick);
			}
		}
		if (this.hintKey != null) {
			int hintY = FIRST_ROW_Y + this.hintTop - this.pane.offset();
			MenuText.centered(I18n.get(this.hintKey), this.width / 2, hintY, 0xA0A0A0);
		}
		this.pane.endClip();
		this.pane.renderScrollbar();
		if (this.shortResetButton != null && this.shortResetButton.isMouseOver(mouseX, mouseY)) {
			renderTooltip(I18n.get("gui.tntsallin1client.reset.button"), mouseX, mouseY);
		}
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

		AbstractWidget createButton(int x, int width) {
			// Clicked by the screen itself, not by the game - so the button needs no action of its own.
			return new Button(x, 0, width, ROW_HEIGHT, this.label.get(), pressed -> { });
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
