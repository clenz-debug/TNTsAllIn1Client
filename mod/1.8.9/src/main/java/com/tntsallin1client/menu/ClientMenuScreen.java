package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;

/**
 * The ingame mod menu: one on/off switch per feature, grouped under section headings. A feature
 * with more to configure than the switch gets its own options screen, opened via the small button
 * next to its switch. Reachable via the title screen and pause menu buttons ({@link MenuButtons}) or
 * its own key binding ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 *
 * <p>The rows scroll between the search field and the "Done" button once they no longer fit (see
 * {@link ScrollPane}). Typing into the search field leaves only the rows whose name contains the
 * text; a section none of whose rows are left drops its heading too.
 */
public class ClientMenuScreen extends ClientScreen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int HEADER_HEIGHT = 18;
	private static final int SEARCH_FIELD_Y = 30;
	private static final int SEARCH_FIELD_ID = 0;
	private static final int VIEWPORT_TOP = SEARCH_FIELD_Y + ROW_HEIGHT + 6;
	/** Distance from the screen's bottom edge to where the rows end - leaves room for "Done". */
	private static final int VIEWPORT_BOTTOM_MARGIN = 32;
	private static final int LEFT_MOUSE_BUTTON = 0;
	private static final int DONE_BUTTON_ID = 0;
	/** A row's switch gets this plus the row's index in {@link #clickable}, the button opening its screen {@link #FIRST_SCREEN_ID} plus the same. */
	private static final int FIRST_TOGGLE_ID = 100;
	private static final int FIRST_SCREEN_ID = 1000;

	private final List<Row> rows = new ArrayList<Row>();
	/** Every row that isn't a heading. */
	private final List<Row> clickable = new ArrayList<Row>();
	/** Lives as long as the screen, so the list doesn't jump to the top after a visit to an options screen. */
	private final ScrollPane pane = new ScrollPane(this::placeRows);
	private TextFieldWidget searchField;
	/** Kept across a visit to an options screen - coming back builds a new, empty search field. */
	private String searchQuery = "";

	public ClientMenuScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.menu.title");

		final ClientConfig config = ClientConfig.get();
		addSection("gui.tntsallin1client.menu.section_hud");
		addFeature("gui.tntsallin1client.menu.coordinates_hud", () -> config.coordinatesHudEnabled, value -> config.coordinatesHudEnabled = value,
				() -> new CoordinatesHudOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.item_counter", () -> config.itemCounterEnabled, value -> config.itemCounterEnabled = value,
				() -> new ItemCounterOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.fps_counter", () -> config.fpsCounterEnabled, value -> config.fpsCounterEnabled = value,
				() -> new FpsCounterOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.latency_hud", () -> config.latencyHudEnabled, value -> config.latencyHudEnabled = value,
				() -> new LatencyOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.clock_hud", () -> config.clockHudEnabled, value -> config.clockHudEnabled = value,
				() -> new ClockOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.keystrokes", () -> config.keystrokesEnabled, value -> config.keystrokesEnabled = value,
				() -> new KeystrokesOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.armor_status", () -> config.armorStatusEnabled, value -> config.armorStatusEnabled = value,
				() -> new ArmorStatusOptionsScreen(this));

		addSection("gui.tntsallin1client.menu.section_rendering");
		addFeature("gui.tntsallin1client.menu.zoom", () -> config.zoomEnabled, value -> config.zoomEnabled = value,
				() -> new ZoomOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.crosshair", () -> config.customCrosshairEnabled, value -> config.customCrosshairEnabled = value,
				() -> new CrosshairOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.fullbright", () -> config.fullbrightEnabled, value -> config.fullbrightEnabled = value, null);

		addSection("gui.tntsallin1client.menu.section_misc");
		addLink("gui.tntsallin1client.menu.hud_editor_button", () -> new HudEditorScreen(this));
	}

	private void addSection(String labelKey) {
		this.rows.add(new Row(labelKey, null, null, null));
	}

	/** `optionsScreen` may be null for a feature that is nothing but its switch. */
	private void addFeature(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Screen> optionsScreen) {
		addClickable(new Row(labelKey, getter, setter, optionsScreen));
	}

	/** A row that is one button opening another screen. */
	private void addLink(String labelKey, Supplier<Screen> screen) {
		addClickable(new Row(labelKey, null, null, screen));
	}

	private void addClickable(Row row) {
		this.rows.add(row);
		this.clickable.add(row);
	}

	@Override
	public void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		// The field's frame lies one pixel outside the box given here.
		this.searchField = new TextFieldWidget(SEARCH_FIELD_ID, this.textRenderer, x + 1, SEARCH_FIELD_Y + 1, ROW_WIDTH - 2, ROW_HEIGHT - 2);
		this.searchField.setText(this.searchQuery);
		// Holding a key (backspace) repeats it while this screen is open.
		Keyboard.enableRepeatEvents(true);

		for (Row row : this.rows) {
			row.toggle = null;
			row.open = null;
			if (row.isHeader()) {
				continue;
			}
			int index = this.clickable.indexOf(row);
			if (row.isLink()) {
				row.open = new ButtonWidget(FIRST_SCREEN_ID + index, x, 0, ROW_WIDTH, ROW_HEIGHT, I18n.translate(row.labelKey));
			} else {
				boolean hasOptions = row.screen != null;
				int toggleWidth = hasOptions ? ROW_WIDTH - OPTIONS_BUTTON_WIDTH - TOGGLE_GAP : ROW_WIDTH;
				row.toggle = new ButtonWidget(FIRST_TOGGLE_ID + index, x, 0, toggleWidth, ROW_HEIGHT, row.toggleLabel());
				if (hasOptions) {
					row.open = new ButtonWidget(FIRST_SCREEN_ID + index, x + toggleWidth + TOGGLE_GAP, 0, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT,
							I18n.translate("gui.tntsallin1client.menu.options_button"));
				}
			}
		}
		layoutRows();

		this.buttons.add(new ButtonWidget(DONE_BUTTON_ID, x, this.height - 28, ROW_WIDTH, ROW_HEIGHT, I18n.translate("gui.done")));
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	/** Decides which rows the search leaves and stacks those from the top. */
	private void layoutRows() {
		String needle = this.searchQuery.trim().toLowerCase(Locale.ROOT);
		Row header = null;
		for (Row row : this.rows) {
			if (row.isHeader()) {
				header = row;
				row.visible = false;
			} else {
				row.visible = needle.isEmpty() || I18n.translate(row.labelKey).toLowerCase(Locale.ROOT).contains(needle);
				if (row.visible && header != null) {
					header.visible = true;
				}
			}
		}

		int y = 0;
		int contentHeight = 0;
		for (Row row : this.rows) {
			if (!row.visible) {
				continue;
			}
			row.y = y;
			if (row.isHeader()) {
				y += HEADER_HEIGHT;
				contentHeight = y;
			} else {
				contentHeight = y + ROW_HEIGHT;
				y += ROW_SPACING;
			}
		}
		int x = (this.width - ROW_WIDTH) / 2;
		this.pane.layout(VIEWPORT_TOP, this.height - VIEWPORT_BOTTOM_MARGIN, x + ROW_WIDTH + ScrollPane.SCROLLBAR_GAP, contentHeight);
	}

	private void placeRows() {
		for (Row row : this.rows) {
			int y = screenY(row);
			if (row.toggle != null) {
				row.toggle.y = y;
			}
			if (row.open != null) {
				row.open.y = y;
			}
		}
	}

	private int screenY(Row row) {
		return VIEWPORT_TOP + row.y - this.pane.offset();
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == DONE_BUTTON_ID) {
			back();
		} else if (button.id >= FIRST_SCREEN_ID && button.id < FIRST_SCREEN_ID + this.clickable.size()) {
			this.client.setScreen(this.clickable.get(button.id - FIRST_SCREEN_ID).screen.get());
		} else if (button.id >= FIRST_TOGGLE_ID && button.id < FIRST_TOGGLE_ID + this.clickable.size()) {
			Row row = this.clickable.get(button.id - FIRST_TOGGLE_ID);
			row.setter.accept(!row.getter.getAsBoolean());
			ClientConfig.get().save();
			button.message = row.toggleLabel();
		}
	}

	@Override
	protected void keyPressed(char character, int code) {
		if (this.searchField.isFocused() && code != Keyboard.KEY_ESCAPE) {
			this.searchField.keyPressed(character, code);
			if (!this.searchField.getText().equals(this.searchQuery)) {
				this.searchQuery = this.searchField.getText();
				layoutRows();
			}
			return;
		}
		super.keyPressed(character, code);
	}

	@Override
	public void tick() {
		super.tick();
		// Keeps the text cursor blinking.
		this.searchField.tick();
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		this.pane.handleWheel();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		// The field takes or drops the keyboard focus depending on whether the click hit it.
		this.searchField.mouseClicked(mouseX, mouseY, button);
		if (button == LEFT_MOUSE_BUTTON && this.pane.contains(mouseY)) {
			if (this.pane.mouseClicked(mouseX, mouseY)) {
				return;
			}
			for (Row row : this.rows) {
				ButtonWidget clicked = row.buttonAt(this, mouseX, mouseY);
				if (clicked != null) {
					clicked.playDownSound(this.client.getSoundManager());
					buttonClicked(clicked);
					return;
				}
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long timeSinceClick) {
		this.pane.mouseDragged(mouseY);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		this.pane.mouseReleased();
		super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);

		this.searchField.render();
		if (this.searchQuery.isEmpty() && !this.searchField.isFocused()) {
			this.drawWithShadow(this.textRenderer, I18n.translate("gui.tntsallin1client.menu.search"),
					this.searchField.x + 4, this.searchField.y + (ROW_HEIGHT - 2 - this.textRenderer.fontHeight) / 2, 0x707070);
		}

		// A button scrolled half out of view must not light up under a cursor that is on the search field or on "Done".
		int hoverY = this.pane.contains(mouseY) ? mouseY : -1;
		this.pane.beginClip(this.client);
		for (Row row : this.rows) {
			if (!row.visible) {
				continue;
			}
			if (row.isHeader()) {
				this.drawCenteredString(this.textRenderer, I18n.translate(row.labelKey), this.width / 2, screenY(row) + 4, 0xA0A0A0);
			}
			if (row.toggle != null) {
				row.toggle.render(this.client, mouseX, hoverY);
			}
			if (row.open != null) {
				row.open.render(this.client, mouseX, hoverY);
			}
		}
		this.pane.endClip();
		this.pane.renderScrollbar();
	}

	/**
	 * A section heading (nothing but a label), a feature's on/off switch with its optional options
	 * screen, or a link (no switch, just a screen to open).
	 */
	private static final class Row {
		final String labelKey;
		final BooleanSupplier getter;
		final Consumer<Boolean> setter;
		final Supplier<Screen> screen;
		/** Whether the search leaves the row; a heading, whether it leaves any row of its section. */
		boolean visible = true;
		/** Top edge, counted from the top of the rows shown. */
		int y;
		/** The row's on/off switch, if it has one. */
		ButtonWidget toggle;
		/** The button opening the row's screen, if it has one. */
		ButtonWidget open;

		Row(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Screen> screen) {
			this.labelKey = labelKey;
			this.getter = getter;
			this.setter = setter;
			this.screen = screen;
		}

		boolean isHeader() {
			return this.getter == null && this.screen == null;
		}

		boolean isLink() {
			return this.getter == null && this.screen != null;
		}

		String toggleLabel() {
			return MenuText.onOff(this.labelKey, this.getter.getAsBoolean());
		}

		ButtonWidget buttonAt(ClientMenuScreen menu, int mouseX, int mouseY) {
			if (!this.visible) {
				return null;
			}
			if (this.toggle != null && this.toggle.isMouseOver(menu.client, mouseX, mouseY)) {
				return this.toggle;
			}
			if (this.open != null && this.open.isMouseOver(menu.client, mouseX, mouseY)) {
				return this.open;
			}
			return null;
		}
	}
}
