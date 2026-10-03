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
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The ingame mod menu: one on/off switch per feature, grouped under section headings. A feature
 * with more to configure than the switch gets its own options screen, opened via the small button
 * next to its switch. Reachable via the title screen and pause menu buttons ({@link MenuButtons}) or
 * its own key binding ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 *
 * <p>The rows scroll between the title and the "Done" button (mouse wheel or the bar next to them)
 * once they no longer fit. Their buttons are therefore not in the screen's own button list - the
 * game would draw them over the title and take clicks on the parts scrolled out of view - but are
 * drawn clipped and clicked from here.
 */
public class ClientMenuScreen extends ClientScreen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int HEADER_HEIGHT = 18;
	private static final int VIEWPORT_TOP = 30;
	/** Distance from the screen's bottom edge to where the rows end - leaves room for "Done". */
	private static final int VIEWPORT_BOTTOM_MARGIN = 32;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int MIN_THUMB_HEIGHT = 32;
	private static final int LEFT_MOUSE_BUTTON = 0;
	private static final int DONE_BUTTON_ID = 0;
	/** A row's switch gets this plus the row's index in {@link #clickable}, the button opening its screen {@link #FIRST_SCREEN_ID} plus the same. */
	private static final int FIRST_TOGGLE_ID = 100;
	private static final int FIRST_SCREEN_ID = 1000;

	private final List<Row> rows = new ArrayList<Row>();
	/** Every row that isn't a heading. */
	private final List<Row> clickable = new ArrayList<Row>();
	/** Survives opening an options screen and coming back, so the list doesn't jump to the top. */
	private int scrollOffset;
	private int maxScroll;
	private boolean draggingScrollbar;
	/** Where on the scrollbar's thumb it was grabbed. */
	private int thumbGrabOffset;

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
		addFeature("gui.tntsallin1client.menu.keystrokes", () -> config.keystrokesEnabled, value -> config.keystrokesEnabled = value,
				() -> new KeystrokesOptionsScreen(this));

		addSection("gui.tntsallin1client.menu.section_rendering");
		addFeature("gui.tntsallin1client.menu.zoom", () -> config.zoomEnabled, value -> config.zoomEnabled = value,
				() -> new ZoomOptionsScreen(this));

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
		int x = rowsLeft();
		int y = 0;
		int contentHeight = 0;
		for (Row row : this.rows) {
			row.y = y;
			row.toggle = null;
			row.open = null;
			if (row.isHeader()) {
				y += HEADER_HEIGHT;
				contentHeight = y;
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
			contentHeight = y + ROW_HEIGHT;
			y += ROW_SPACING;
		}
		this.maxScroll = Math.max(0, contentHeight - viewportHeight());
		this.draggingScrollbar = false;
		scrollTo(this.scrollOffset);

		this.buttons.add(new ButtonWidget(DONE_BUTTON_ID, x, this.height - 28, ROW_WIDTH, ROW_HEIGHT, I18n.translate("gui.done")));
	}

	private int rowsLeft() {
		return (this.width - ROW_WIDTH) / 2;
	}

	private int viewportBottom() {
		return this.height - VIEWPORT_BOTTOM_MARGIN;
	}

	private int viewportHeight() {
		return viewportBottom() - VIEWPORT_TOP;
	}

	private boolean inViewport(int mouseY) {
		return mouseY >= VIEWPORT_TOP && mouseY < viewportBottom();
	}

	/** Scrolls the rows so that `offset` pixels of them lie above the visible part. */
	private void scrollTo(int offset) {
		this.scrollOffset = MathHelper.clamp(offset, 0, this.maxScroll);
		for (Row row : this.rows) {
			int y = VIEWPORT_TOP + row.y - this.scrollOffset;
			if (row.toggle != null) {
				row.toggle.y = y;
			}
			if (row.open != null) {
				row.open.y = y;
			}
		}
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

	/** The game hands a screen clicks and drags but not the wheel - that has to be read here. */
	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			scrollTo(this.scrollOffset - Integer.signum(wheel) * SCROLL_STEP);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == LEFT_MOUSE_BUTTON && inViewport(mouseY)) {
			if (this.maxScroll > 0 && mouseX >= scrollbarX() && mouseX < scrollbarX() + SCROLLBAR_WIDTH) {
				// Grabbed on the thumb it keeps that spot under the cursor, clicked beside it the thumb jumps there.
				boolean onThumb = mouseY >= thumbY() && mouseY < thumbY() + thumbHeight();
				this.thumbGrabOffset = onThumb ? mouseY - thumbY() : thumbHeight() / 2;
				this.draggingScrollbar = true;
				dragScrollbarTo(mouseY);
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
		if (this.draggingScrollbar) {
			dragScrollbarTo(mouseY);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		this.draggingScrollbar = false;
		super.mouseReleased(mouseX, mouseY, button);
	}

	private void dragScrollbarTo(int mouseY) {
		int thumbTravel = viewportHeight() - thumbHeight();
		scrollTo(Math.round((mouseY - this.thumbGrabOffset - VIEWPORT_TOP) * (float) this.maxScroll / thumbTravel));
	}

	private int scrollbarX() {
		return rowsLeft() + ROW_WIDTH + SCROLLBAR_GAP;
	}

	private int thumbHeight() {
		int viewportHeight = viewportHeight();
		return MathHelper.clamp(viewportHeight * viewportHeight / (viewportHeight + this.maxScroll), MIN_THUMB_HEIGHT, viewportHeight - 8);
	}

	private int thumbY() {
		return VIEWPORT_TOP + this.scrollOffset * (viewportHeight() - thumbHeight()) / this.maxScroll;
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);

		// A button scrolled half out of view must not light up under a cursor that is on the title or on "Done".
		int hoverY = inViewport(mouseY) ? mouseY : -1;
		// The clip area is given in window pixels, counted from the bottom edge.
		int scale = new Window(this.client).getScaleFactor();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(0, this.client.height - viewportBottom() * scale, this.client.width, viewportHeight() * scale);
		for (Row row : this.rows) {
			if (row.isHeader()) {
				this.drawCenteredString(this.textRenderer, I18n.translate(row.labelKey), this.width / 2, VIEWPORT_TOP + row.y - this.scrollOffset + 4, 0xA0A0A0);
			}
			if (row.toggle != null) {
				row.toggle.render(this.client, mouseX, hoverY);
			}
			if (row.open != null) {
				row.open.render(this.client, mouseX, hoverY);
			}
		}
		GL11.glDisable(GL11.GL_SCISSOR_TEST);

		if (this.maxScroll > 0) {
			// Same look as the scrollbar of the game's own lists.
			int x = scrollbarX();
			int thumbY = thumbY();
			int thumbHeight = thumbHeight();
			fill(x, VIEWPORT_TOP, x + SCROLLBAR_WIDTH, viewportBottom(), 0xFF000000);
			fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF808080);
			fill(x, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight - 1, 0xFFC0C0C0);
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
		/** Top edge, counted from the top of the whole list. */
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
