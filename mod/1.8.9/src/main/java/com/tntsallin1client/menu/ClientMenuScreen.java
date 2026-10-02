package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * The ingame mod menu: one on/off switch per feature, grouped under section headings. A feature
 * with more to configure than the switch gets its own options screen, opened via the small button
 * next to its switch. Reachable via the title screen and pause menu buttons ({@link MenuButtons}) or
 * its own key binding ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 *
 * <p>The rows are laid out top to bottom without scrolling - fine for the few features this version
 * has so far, needs a scrolling list once they no longer fit on screen.
 */
public class ClientMenuScreen extends ClientScreen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int HEADER_HEIGHT = 18;
	private static final int FIRST_ROW_Y = 36;
	private static final int DONE_BUTTON_ID = 0;
	/** A row's switch gets this plus the row's index in {@link #clickable}, the button opening its screen {@link #FIRST_SCREEN_ID} plus the same. */
	private static final int FIRST_TOGGLE_ID = 100;
	private static final int FIRST_SCREEN_ID = 1000;

	private final List<Row> rows = new ArrayList<Row>();
	/** Every row that isn't a heading. */
	private final List<Row> clickable = new ArrayList<Row>();

	public ClientMenuScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.menu.title");

		final ClientConfig config = ClientConfig.get();
		addSection("gui.tntsallin1client.menu.section_hud");
		addFeature("gui.tntsallin1client.menu.coordinates_hud", () -> config.coordinatesHudEnabled, value -> config.coordinatesHudEnabled = value,
				() -> new CoordinatesHudOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.fps_counter", () -> config.fpsCounterEnabled, value -> config.fpsCounterEnabled = value,
				() -> new FpsCounterOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.latency_hud", () -> config.latencyHudEnabled, value -> config.latencyHudEnabled = value,
				() -> new LatencyOptionsScreen(this));
		addFeature("gui.tntsallin1client.menu.clock_hud", () -> config.clockHudEnabled, value -> config.clockHudEnabled = value,
				() -> new ClockOptionsScreen(this));

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
		int y = FIRST_ROW_Y;
		for (Row row : this.rows) {
			row.y = y;
			if (row.isHeader()) {
				y += HEADER_HEIGHT;
				continue;
			}
			int index = this.clickable.indexOf(row);
			if (row.isLink()) {
				this.buttons.add(new ButtonWidget(FIRST_SCREEN_ID + index, x, y, ROW_WIDTH, ROW_HEIGHT, I18n.translate(row.labelKey)));
			} else {
				boolean hasOptions = row.screen != null;
				int toggleWidth = hasOptions ? ROW_WIDTH - OPTIONS_BUTTON_WIDTH - TOGGLE_GAP : ROW_WIDTH;
				this.buttons.add(new ButtonWidget(FIRST_TOGGLE_ID + index, x, y, toggleWidth, ROW_HEIGHT, row.toggleLabel()));
				if (hasOptions) {
					this.buttons.add(new ButtonWidget(FIRST_SCREEN_ID + index, x + toggleWidth + TOGGLE_GAP, y, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT,
							I18n.translate("gui.tntsallin1client.menu.options_button")));
				}
			}
			y += ROW_SPACING;
		}
		this.buttons.add(new ButtonWidget(DONE_BUTTON_ID, x, this.height - 28, ROW_WIDTH, ROW_HEIGHT, I18n.translate("gui.done")));
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
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);
		for (Row row : this.rows) {
			if (row.isHeader()) {
				this.drawCenteredString(this.textRenderer, I18n.translate(row.labelKey), this.width / 2, row.y + 4, 0xA0A0A0);
			}
		}
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
		int y;

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
	}
}
