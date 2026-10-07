package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.tour.TourRect;
import com.tntsallin1client.tour.TourTargets;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

/**
 * The ingame mod menu in the Minecraft design (what is in it comes from {@link ClientMenuFeatures}):
 * one on/off switch per feature, grouped under section headings. A feature with more to configure
 * than the switch gets its own options screen, opened via the small button next to its switch.
 * Reachable via the title screen and pause menu buttons ({@link MenuButtons}) or its own key binding
 * ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 *
 * <p>The rows scroll between the search field and the "Done" button once they no longer fit (see
 * {@link ScrollPane}). Typing into the search field leaves only the rows whose name contains the
 * text; a section none of whose rows are left drops its heading too.
 */
public class ClientMenuScreen extends ClientScreen implements FeatureSink, TourTargets {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int HEADER_HEIGHT = 18;
	private static final int SEARCH_FIELD_Y = 30;
	private static final int VIEWPORT_TOP = SEARCH_FIELD_Y + ROW_HEIGHT + 6;
	/** Distance from the screen's bottom edge to where the rows end - leaves room for "Done". */
	private static final int VIEWPORT_BOTTOM_MARGIN = 32;
	private static final int LEFT_MOUSE_BUTTON = 0;

	private final List<Row> rows = new ArrayList<Row>();
	/** Lives as long as the screen, so the list doesn't jump to the top after a visit to an options screen. */
	private final ScrollPane pane = new ScrollPane(this::placeRows);
	private EditBox searchField;
	/** Kept across a visit to an options screen - coming back builds a new, empty search field. */
	private String searchQuery = "";

	public ClientMenuScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.menu.title");
		ClientMenuFeatures.populate(this, this);
	}

	@Override
	public void beginSection(String labelKey) {
		this.rows.add(new Row(labelKey, null, null, null));
	}

	/** `optionsScreen` may be null for a feature that is nothing but its switch. */
	@Override
	public void addFeature(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Screen> optionsScreen) {
		this.rows.add(new Row(labelKey, getter, setter, optionsScreen));
	}

	/** A row that is one button opening another screen. */
	@Override
	public void addLink(LinkRole role, String labelKey, Supplier<Screen> screen) {
		this.rows.add(new Row(labelKey, null, null, screen));
	}

	@Override
	protected void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		// The field's frame lies one pixel outside the box given here.
		this.searchField = new EditBox(this.font, x + 1, SEARCH_FIELD_Y + 1, ROW_WIDTH - 2, ROW_HEIGHT - 2, I18n.get("gui.tntsallin1client.menu.search"));
		this.searchField.setValue(this.searchQuery);
		// Holding a key (backspace) repeats it while this screen is open.
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

		for (Row row : this.rows) {
			row.toggle = null;
			row.open = null;
			if (row.isHeader()) {
				continue;
			}
			// Clicked by the screen itself, not by the game - so the buttons need no action of their own.
			if (row.isLink()) {
				row.open = new Button(x, 0, ROW_WIDTH, ROW_HEIGHT, I18n.get(row.labelKey), pressed -> { });
			} else {
				boolean hasOptions = row.screen != null;
				int toggleWidth = hasOptions ? ROW_WIDTH - OPTIONS_BUTTON_WIDTH - TOGGLE_GAP : ROW_WIDTH;
				row.toggle = new Button(x, 0, toggleWidth, ROW_HEIGHT, row.toggleLabel(), pressed -> { });
				if (hasOptions) {
					row.open = new Button(x + toggleWidth + TOGGLE_GAP, 0, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT,
							I18n.get("gui.tntsallin1client.menu.options_button"), pressed -> { });
				}
			}
		}
		layoutRows();

		this.addButton(new Button(x, this.height - 28, ROW_WIDTH, ROW_HEIGHT, I18n.get("gui.done"), pressed -> back()));
	}

	@Override
	public void removed() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
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
				row.visible = needle.isEmpty() || I18n.get(row.labelKey).toLowerCase(Locale.ROOT).contains(needle);
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

	// --- In-game tour ------------------------------------------------------------------------

	@Override
	public TourRect tourTarget(String name) {
		if (this.searchField == null) {
			return null;
		}
		if (name.equals(SEARCH)) {
			return new TourRect(this.searchField.x - 1, this.searchField.y - 1, ROW_WIDTH, ROW_HEIGHT);
		}
		if (name.equals(HUD_EDITOR)) {
			return rowBounds("gui.tntsallin1client.menu.hud_editor_button", null);
		}
		if (name.startsWith(FEATURE_OPTIONS)) {
			return rowBounds(name.substring(FEATURE_OPTIONS.length()), Boolean.FALSE);
		}
		if (name.startsWith(FEATURE_SWITCH)) {
			return rowBounds(name.substring(FEATURE_SWITCH.length()), Boolean.TRUE);
		}
		if (name.startsWith(FEATURE)) {
			return rowBounds(name.substring(FEATURE.length()), null);
		}
		return null;
	}

	/**
	 * A row (by its translation key) - `part` true: its switch, false: its Options button, null: all
	 * of it. A row outside the viewport gets scrolled to instead.
	 */
	private TourRect rowBounds(String labelKey, Boolean part) {
		for (Row row : this.rows) {
			if (!row.labelKey.equals(labelKey) || !row.visible || row.isHeader()) {
				continue;
			}
			int top = screenY(row);
			if (top < VIEWPORT_TOP || top + ROW_HEIGHT > this.height - VIEWPORT_BOTTOM_MARGIN) {
				this.pane.scrollTo(row.y);
				return null;
			}
			Button button = part == null ? null : part ? row.toggle : row.open;
			if (part != null && button == null) {
				return null;
			}
			return button != null
					? new TourRect(button.x, top, button.getWidth(), ROW_HEIGHT)
					: new TourRect((this.width - ROW_WIDTH) / 2, top, ROW_WIDTH, ROW_HEIGHT);
		}
		return null;
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

	private void rowClicked(Row row, Button button) {
		if (button == row.open) {
			this.minecraft.setScreen(row.screen.get());
		} else {
			row.setter.accept(!row.getter.getAsBoolean());
			ClientConfig.get().save();
			button.setMessage(row.toggleLabel());
		}
	}

	private void searchChanged() {
		if (!this.searchField.getValue().equals(this.searchQuery)) {
			this.searchQuery = this.searchField.getValue();
			layoutRows();
		}
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (this.searchField.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
			this.searchField.keyPressed(key, scanCode, modifiers);
			searchChanged();
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		if (this.searchField.isFocused()) {
			this.searchField.charTyped(character, modifiers);
			searchChanged();
			return true;
		}
		return super.charTyped(character, modifiers);
	}

	@Override
	public void tick() {
		super.tick();
		// Keeps the text cursor blinking.
		this.searchField.tick();
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
		// The field takes or drops the keyboard focus depending on whether the click hit it.
		boolean onField = this.searchField.isMouseOver(x, y);
		this.searchField.setFocus(onField);
		if (onField) {
			this.searchField.mouseClicked(x, y, button);
			return true;
		}
		if (button == LEFT_MOUSE_BUTTON && this.pane.contains(mouseY)) {
			if (this.pane.mouseClicked(mouseX, mouseY)) {
				return true;
			}
			for (Row row : this.rows) {
				Button clicked = row.buttonAt(x, y);
				if (clicked != null) {
					clicked.playDownSound(this.minecraft.getSoundManager());
					rowClicked(row, clicked);
					return true;
				}
			}
		}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
		this.pane.mouseDragged((int) y);
		return super.mouseDragged(x, y, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		this.pane.mouseReleased();
		return super.mouseReleased(x, y, button);
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		super.render(mouseX, mouseY, partialTick);
		this.searchField.render(mouseX, mouseY, partialTick);
		if (this.searchQuery.isEmpty() && !this.searchField.isFocused()) {
			this.drawString(this.font, I18n.get("gui.tntsallin1client.menu.search"),
					this.searchField.x + 4, this.searchField.y + (ROW_HEIGHT - 2 - this.font.lineHeight) / 2, 0x707070);
		}

		// A button scrolled half out of view must not light up under a cursor that is on the search field or on "Done".
		int hoverY = this.pane.contains(mouseY) ? mouseY : -1;
		this.pane.beginClip(this.minecraft);
		for (Row row : this.rows) {
			if (!row.visible) {
				continue;
			}
			if (row.isHeader()) {
				this.drawCenteredString(this.font, I18n.get(row.labelKey), this.width / 2, screenY(row) + 4, 0xA0A0A0);
			}
			if (row.toggle != null) {
				row.toggle.render(mouseX, hoverY, partialTick);
			}
			if (row.open != null) {
				row.open.render(mouseX, hoverY, partialTick);
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
		Button toggle;
		/** The button opening the row's screen, if it has one. */
		Button open;

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

		Button buttonAt(double mouseX, double mouseY) {
			if (!this.visible) {
				return null;
			}
			if (this.toggle != null && this.toggle.isMouseOver(mouseX, mouseY)) {
				return this.toggle;
			}
			if (this.open != null && this.open.isMouseOver(mouseX, mouseY)) {
				return this.open;
			}
			return null;
		}
	}
}
