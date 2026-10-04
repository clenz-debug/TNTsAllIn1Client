package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.FeatureIcons;
import com.tntsallin1client.tour.TourRect;
import com.tntsallin1client.tour.TourScreen;
import com.tntsallin1client.tour.TourTargets;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;

/**
 * The Client Mods menu in the client design, as in the Fabric versions: Done / Search / Move-Resize
 * along the top ("Reset All" below Move-Resize), then every feature as a card - a symbol with the
 * feature's name under it on top, an Options strip in the middle that opens the feature's options
 * screen (greyed out when it has none), and an Enabled/Disabled bar at the bottom that toggles it.
 * Same entries as the Minecraft-design list ({@link ClientMenuFeatures}), grouped by the same
 * sections.
 *
 * <p>The symbols come from {@link FeatureIcons}; features without one show their name large and
 * centered in the image area instead.
 */
public class ClientModsCardScreen extends Screen implements FeatureSink, TourTargets, TourScreen.Closable {
	private static final int MARGIN = 10;
	private static final int TOP_BAR_Y = 8;
	private static final int TOP_BAR_HEIGHT = 20;
	private static final int TOP_BAR_BUTTON_WIDTH = 80;
	/** "Reset All" sits below "Move/Resize" in the top right corner - the cards start below both. */
	private static final int SECOND_ROW_Y = TOP_BAR_Y + TOP_BAR_HEIGHT + 4;
	private static final int CONTENT_TOP = SECOND_ROW_Y + TOP_BAR_HEIGHT + 6;
	private static final int FOOTER_HEIGHT = 30;
	private static final int FOOTER_BUTTON_WIDTH = 120;
	private static final int CARD_WIDTH = 110;
	private static final int CARD_GAP = 10;
	private static final int IMAGE_HEIGHT = 48;
	private static final int ICON_SIZE = 28;
	private static final int ICON_TOP = 4;
	/** Scales tried for the name on cards without a symbol - the first one it fits at wins. */
	private static final float[] LARGE_NAME_SCALES = {1.5F, 1.0F};
	private static final int OPTIONS_HEIGHT = 14;
	private static final int STATUS_HEIGHT = 16;
	private static final int CARD_HEIGHT = IMAGE_HEIGHT + OPTIONS_HEIGHT + STATUS_HEIGHT;
	private static final int SECTION_HEADER_HEIGHT = 16;
	/** Right of the cards, in the screen's margin. */
	private static final int SCROLLBAR_FROM_RIGHT = 8;

	private static final int LEFT_MOUSE_BUTTON = 0;
	private static final int DONE_BUTTON_ID = 0;
	private static final int HUD_EDITOR_BUTTON_ID = 1;
	private static final int RESET_ALL_BUTTON_ID = 2;
	/** A footer button gets this plus its index in {@link #footerLinks}. */
	private static final int FIRST_FOOTER_ID = 100;
	private static final int SEARCH_FIELD_ID = 0;

	private final Screen parent;
	private final List<Section> sections = new ArrayList<Section>();
	private final List<Link> footerLinks = new ArrayList<Link>();
	private Supplier<Screen> hudEditor;
	private Supplier<Screen> resetAll;

	/** Lives as long as the screen, so the cards don't jump to the top after a visit to an options screen. */
	private final ScrollPane pane = new ScrollPane(() -> { });
	private TextFieldWidget searchField;
	/** Kept across a visit to an options screen - coming back builds a new, empty search field. */
	private String searchQuery = "";
	/** Only there to make the click sound a button makes. */
	private final ButtonWidget clickSound = new ButtonWidget(-1, 0, 0, "");

	public ClientModsCardScreen(Screen parent) {
		this.parent = parent;
		ClientMenuFeatures.populate(this, this);
	}

	// --- FeatureSink --------------------------------------------------------------------------

	@Override
	public void beginSection(String labelKey) {
		this.sections.add(new Section(labelKey));
	}

	@Override
	public void addFeature(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Screen> optionsScreen) {
		this.sections.get(this.sections.size() - 1).cards.add(new Card(labelKey, getter, setter, optionsScreen));
	}

	@Override
	public void addLink(LinkRole role, String labelKey, Supplier<Screen> screen) {
		if (role == LinkRole.HUD_EDITOR) {
			this.hudEditor = screen;
		} else if (role == LinkRole.RESET_ALL) {
			this.resetAll = screen;
		} else {
			this.footerLinks.add(new Link(labelKey, screen));
		}
	}

	// --- Layout ------------------------------------------------------------------------------

	@Override
	public void init() {
		// The buttons are the game's own - on this screen `ButtonWidgetMixin` draws them in the theme.
		this.buttons.add(new ButtonWidget(DONE_BUTTON_ID, MARGIN, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH, TOP_BAR_HEIGHT, I18n.translate("gui.done")));
		int right = this.width - MARGIN - TOP_BAR_BUTTON_WIDTH;
		if (this.hudEditor != null) {
			this.buttons.add(new ButtonWidget(HUD_EDITOR_BUTTON_ID, right, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH, TOP_BAR_HEIGHT,
					I18n.translate("gui.tntsallin1client.cards.move_resize")));
		}
		if (this.resetAll != null) {
			this.buttons.add(new ButtonWidget(RESET_ALL_BUTTON_ID, right, SECOND_ROW_Y, TOP_BAR_BUTTON_WIDTH, TOP_BAR_HEIGHT,
					I18n.translate("gui.tntsallin1client.cards.reset_all")));
		}

		int searchWidth = searchWidth();
		// The field's frame lies one pixel outside the box given here.
		this.searchField = new TextFieldWidget(SEARCH_FIELD_ID, this.textRenderer, (this.width - searchWidth) / 2 + 1, TOP_BAR_Y + 1,
				searchWidth - 2, TOP_BAR_HEIGHT - 2);
		this.searchField.setText(this.searchQuery);
		// Holding a key (backspace) repeats it while this screen is open.
		Keyboard.enableRepeatEvents(true);

		int footerX = (this.width - (this.footerLinks.size() * (FOOTER_BUTTON_WIDTH + CARD_GAP) - CARD_GAP)) / 2;
		for (int index = 0; index < this.footerLinks.size(); index++) {
			this.buttons.add(new ButtonWidget(FIRST_FOOTER_ID + index, footerX, this.height - FOOTER_HEIGHT + 5, FOOTER_BUTTON_WIDTH, TOP_BAR_HEIGHT,
					I18n.translate(this.footerLinks.get(index).labelKey)));
			footerX += FOOTER_BUTTON_WIDTH + CARD_GAP;
		}
		layoutCards();
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	private int searchWidth() {
		return Math.min(200, this.width - 2 * MARGIN - 2 * TOP_BAR_BUTTON_WIDTH - 2 * CARD_GAP);
	}

	private int columns() {
		return Math.max(1, (this.width - 2 * MARGIN + CARD_GAP) / (CARD_WIDTH + CARD_GAP));
	}

	private int gridLeft() {
		return (this.width - (columns() * (CARD_WIDTH + CARD_GAP) - CARD_GAP)) / 2;
	}

	private int viewportBottom() {
		return this.height - FOOTER_HEIGHT;
	}

	/**
	 * Decides which cards the search leaves and where each goes, counted from the top of the cards
	 * shown - a section with no matching card is left out entirely, heading included.
	 */
	private void layoutCards() {
		String needle = this.searchQuery.trim().toLowerCase(Locale.ROOT);
		int columns = columns();
		int left = gridLeft();
		int y = 0;
		for (Section section : this.sections) {
			int shown = 0;
			for (Card card : section.cards) {
				card.visible = needle.isEmpty() || I18n.translate(card.labelKey).toLowerCase(Locale.ROOT).contains(needle);
				if (card.visible) {
					card.x = left + (shown % columns) * (CARD_WIDTH + CARD_GAP);
					card.y = y + SECTION_HEADER_HEIGHT + (shown / columns) * (CARD_HEIGHT + CARD_GAP);
					shown++;
				}
			}
			section.visible = shown > 0;
			if (section.visible) {
				section.y = y;
				y += SECTION_HEADER_HEIGHT + ((shown + columns - 1) / columns) * (CARD_HEIGHT + CARD_GAP);
			}
		}
		this.pane.layout(CONTENT_TOP, viewportBottom(), this.width - SCROLLBAR_FROM_RIGHT, y);
	}

	private int screenY(int contentY) {
		return CONTENT_TOP + contentY - this.pane.offset();
	}

	// --- Rendering ---------------------------------------------------------------------------

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		// The plain theme background - see `ScreenMixin`.
		this.renderBackground();
		ClientTheme theme = ClientTheme.get();

		this.searchField.render();
		if (this.searchQuery.isEmpty() && !this.searchField.isFocused()) {
			ClientFont.draw(I18n.translate("gui.tntsallin1client.menu.search"), this.searchField.x + 4,
					this.searchField.y + (TOP_BAR_HEIGHT - 2 - ClientFont.HEIGHT) / 2.0F, ClientTheme.withAlpha(theme.text, 0.6F));
		}

		this.pane.beginClip(this.client);
		for (Section section : this.sections) {
			if (!section.visible) {
				continue;
			}
			ClientFont.draw(I18n.translate(section.labelKey), gridLeft(), screenY(section.y) + 4, theme.text);
			for (Card card : section.cards) {
				if (!card.visible) {
					continue;
				}
				int top = screenY(card.y);
				boolean hovered = this.pane.contains(mouseY) && mouseX >= card.x && mouseX < card.x + CARD_WIDTH && mouseY >= top && mouseY < top + CARD_HEIGHT;
				renderCard(card, card.x, top, hovered ? mouseY - top : -1);
			}
		}
		this.pane.endClip();
		this.pane.renderScrollbar();

		super.render(mouseX, mouseY, tickDelta);
	}

	/** `hoverY` is the mouse's y inside the card, or -1 when it isn't over it. */
	private void renderCard(Card card, int x, int y, int hoverY) {
		ClientTheme theme = ClientTheme.get();
		boolean optionsHovered = hoverY >= IMAGE_HEIGHT && hoverY < IMAGE_HEIGHT + OPTIONS_HEIGHT && card.options != null;
		boolean statusHovered = hoverY >= IMAGE_HEIGHT + OPTIONS_HEIGHT;
		boolean enabled = card.getter.getAsBoolean();
		String name = I18n.translate(card.labelKey);

		// Image area - the symbol with the name under it, or just the name large for cards without one
		fill(x, y, x + CARD_WIDTH, y + IMAGE_HEIGHT, theme.background1);
		if (!FeatureIcons.has(card.labelKey)) {
			renderLargeName(name, x, y);
		} else {
			FeatureIcons.draw(card.labelKey, x + (CARD_WIDTH - ICON_SIZE) / 2, y + ICON_TOP, ICON_SIZE);
			float titleY = y + ICON_TOP + ICON_SIZE + (IMAGE_HEIGHT - ICON_TOP - ICON_SIZE - ClientFont.HEIGHT) / 2.0F;
			ClientFont.drawCentered(ClientFont.fit(name, CARD_WIDTH - 6), x + CARD_WIDTH / 2.0F, titleY, theme.text);
		}

		// Options strip - greyed out for features without an options screen
		int optionsTop = y + IMAGE_HEIGHT;
		fill(x, optionsTop, x + CARD_WIDTH, optionsTop + OPTIONS_HEIGHT, optionsHovered ? theme.accent1 : theme.background2);
		float optionsAlpha = card.options == null ? 0.3F : optionsHovered ? 1.0F : 0.8F;
		ClientFont.drawCentered(I18n.translate("gui.tntsallin1client.cards.options"), x + CARD_WIDTH / 2.0F,
				optionsTop + (OPTIONS_HEIGHT - ClientFont.HEIGHT) / 2.0F, ClientTheme.withAlpha(theme.text, optionsAlpha));

		// Status bar
		int statusTop = optionsTop + OPTIONS_HEIGHT;
		int statusFill = enabled ? (statusHovered ? theme.accent4 : theme.accent3) : (statusHovered ? theme.accent1 : theme.background2);
		fill(x, statusTop, x + CARD_WIDTH, statusTop + STATUS_HEIGHT, statusFill);
		ClientFont.drawCentered(I18n.translate(enabled ? "gui.tntsallin1client.cards.enabled" : "gui.tntsallin1client.cards.disabled"),
				x + CARD_WIDTH / 2.0F, statusTop + (STATUS_HEIGHT - ClientFont.HEIGHT) / 2.0F, ClientTheme.withAlpha(theme.text, enabled ? 1.0F : 0.6F));

		// Frame and separators
		int frame = hoverY >= 0 ? theme.accent4 : theme.accent2;
		fill(x, y, x + CARD_WIDTH, y + 1, frame);
		fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, frame);
		fill(x, y + 1, x + 1, y + CARD_HEIGHT - 1, frame);
		fill(x + CARD_WIDTH - 1, y + 1, x + CARD_WIDTH, y + CARD_HEIGHT - 1, frame);
		fill(x + 1, optionsTop, x + CARD_WIDTH - 1, optionsTop + 1, frame);
		fill(x + 1, statusTop, x + CARD_WIDTH - 1, statusTop + 1, frame);
	}

	/**
	 * The name word-wrapped (at spaces and after hyphens, so "Inventar-Schnellsortierung" breaks too)
	 * and centered in the image area - at the largest of {@link #LARGE_NAME_SCALES} it fits at.
	 */
	private void renderLargeName(String name, int x, int y) {
		float scale = LARGE_NAME_SCALES[LARGE_NAME_SCALES.length - 1];
		List<String> lines = new ArrayList<String>();
		for (float candidate : LARGE_NAME_SCALES) {
			lines = wrapName(name, CARD_WIDTH - 8, candidate);
			scale = candidate;
			boolean fits = lines.size() * ClientFont.HEIGHT * candidate <= IMAGE_HEIGHT - 4;
			for (String line : lines) {
				fits &= ClientFont.width(line, candidate) <= CARD_WIDTH - 8;
			}
			if (fits) {
				break;
			}
		}
		float lineHeight = ClientFont.HEIGHT * scale;
		float top = y + (IMAGE_HEIGHT - lines.size() * lineHeight) / 2;
		for (int index = 0; index < lines.size(); index++) {
			String line = ClientFont.fit(lines.get(index), CARD_WIDTH - 8, scale);
			ClientFont.draw(line, x + (CARD_WIDTH - ClientFont.width(line, scale)) / 2.0F, top + index * lineHeight, ClientTheme.get().text, scale);
		}
	}

	private static List<String> wrapName(String name, int maxWidth, float scale) {
		List<String> lines = new ArrayList<String>();
		// Pieces keep their leading space / trailing hyphen, so lines are just concatenations
		for (String piece : name.split("(?<=-)|(?= )")) {
			int last = lines.size() - 1;
			if (last >= 0 && ClientFont.width(lines.get(last) + piece, scale) <= maxWidth) {
				lines.set(last, lines.get(last) + piece);
			} else {
				lines.add(piece.trim());
			}
		}
		return lines;
	}

	// --- Input -------------------------------------------------------------------------------

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == DONE_BUTTON_ID) {
			this.client.setScreen(this.parent);
		} else if (button.id == HUD_EDITOR_BUTTON_ID) {
			this.client.setScreen(this.hudEditor.get());
		} else if (button.id == RESET_ALL_BUTTON_ID) {
			// Asks first; the answer comes back through confirmResult.
			this.client.setScreen(this.resetAll.get());
		} else if (button.id >= FIRST_FOOTER_ID && button.id < FIRST_FOOTER_ID + this.footerLinks.size()) {
			this.client.setScreen(this.footerLinks.get(button.id - FIRST_FOOTER_ID).screen.get());
		}
	}

	/** The answer to the question "Reset All" asks. Either way the player is back in the menu afterwards. */
	@Override
	public void confirmResult(boolean confirmed, int id) {
		if (confirmed) {
			ConfigReset.resetAll();
		}
		this.client.setScreen(this);
	}

	@Override
	protected void keyPressed(char character, int code) {
		if (code == Keyboard.KEY_ESCAPE) {
			// Back to where the menu was opened from, not straight into the game.
			this.client.setScreen(this.parent);
			return;
		}
		if (this.searchField.isFocused()) {
			this.searchField.keyPressed(character, code);
			if (!this.searchField.getText().equals(this.searchQuery)) {
				this.searchQuery = this.searchField.getText();
				layoutCards();
			}
		}
	}

	@Override
	public void tick() {
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
			for (Section section : this.sections) {
				for (Card card : section.cards) {
					if (section.visible && card.visible && cardClicked(card, mouseX - card.x, mouseY - screenY(card.y))) {
						return;
					}
				}
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	/** `localX`/`localY`: where in the card the click landed. */
	private boolean cardClicked(Card card, int localX, int localY) {
		if (localX < 0 || localX >= CARD_WIDTH || localY < IMAGE_HEIGHT || localY >= CARD_HEIGHT) {
			return false;
		}
		if (localY >= IMAGE_HEIGHT + OPTIONS_HEIGHT) {
			this.clickSound.playDownSound(this.client.getSoundManager());
			card.setter.accept(!card.getter.getAsBoolean());
			ClientConfig.get().save();
			return true;
		}
		if (card.options != null) {
			this.clickSound.playDownSound(this.client.getSoundManager());
			this.client.setScreen(card.options.get());
			return true;
		}
		return false;
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

	// --- In-game tour ------------------------------------------------------------------------

	@Override
	public void closeForTour() {
		this.client.setScreen(this.parent);
	}

	@Override
	public TourRect tourTarget(String name) {
		if (this.searchField == null) {
			return null;
		}
		TourRect search = new TourRect(this.searchField.x - 1, this.searchField.y - 1, searchWidth(), TOP_BAR_HEIGHT);
		if (name.equals(SEARCH)) {
			return search;
		}
		int right = this.width - MARGIN - TOP_BAR_BUTTON_WIDTH;
		TourRect moveResize = this.hudEditor != null ? new TourRect(right, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH, TOP_BAR_HEIGHT) : null;
		if (name.equals(HUD_EDITOR)) {
			return moveResize;
		}
		if (name.equals(TOP_BAR)) {
			TourRect bar = TourRect.union(new TourRect(MARGIN, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH, TOP_BAR_HEIGHT), TourRect.union(search, moveResize));
			return this.resetAll != null ? TourRect.union(bar, new TourRect(right, SECOND_ROW_Y, TOP_BAR_BUTTON_WIDTH, TOP_BAR_HEIGHT)) : bar;
		}
		if (name.startsWith(FEATURE_OPTIONS)) {
			return cardBounds(name.substring(FEATURE_OPTIONS.length()), IMAGE_HEIGHT, OPTIONS_HEIGHT);
		}
		if (name.startsWith(FEATURE_SWITCH)) {
			return cardBounds(name.substring(FEATURE_SWITCH.length()), IMAGE_HEIGHT + OPTIONS_HEIGHT, STATUS_HEIGHT);
		}
		if (name.startsWith(FEATURE)) {
			return cardBounds(name.substring(FEATURE.length()), 0, CARD_HEIGHT);
		}
		return null;
	}

	/** A strip of a visible card (by its feature's translation key) - a card outside the viewport gets scrolled to instead. */
	private TourRect cardBounds(String key, int stripTop, int stripHeight) {
		for (Section section : this.sections) {
			for (Card card : section.cards) {
				if (!section.visible || !card.visible || !card.labelKey.equals(key)) {
					continue;
				}
				int top = screenY(card.y);
				if (top < CONTENT_TOP || top + CARD_HEIGHT > viewportBottom()) {
					this.pane.scrollTo(card.y);
					return null;
				}
				return new TourRect(card.x, top + stripTop, CARD_WIDTH, stripHeight);
			}
		}
		return null;
	}

	// --- Data --------------------------------------------------------------------------------

	private static final class Section {
		final String labelKey;
		final List<Card> cards = new ArrayList<Card>();
		/** Whether the search leaves any of its cards. */
		boolean visible = true;
		/** Top edge of its heading, counted from the top of the cards shown. */
		int y;

		Section(String labelKey) {
			this.labelKey = labelKey;
		}
	}

	private static final class Card {
		/** The feature's name - its translation key also picks the card's symbol in {@link FeatureIcons}. */
		final String labelKey;
		final BooleanSupplier getter;
		final Consumer<Boolean> setter;
		/** Null for a feature that is nothing but its switch. */
		final Supplier<Screen> options;
		boolean visible = true;
		int x;
		/** Top edge, counted from the top of the cards shown. */
		int y;

		Card(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Screen> options) {
			this.labelKey = labelKey;
			this.getter = getter;
			this.setter = setter;
			this.options = options;
		}
	}

	private static final class Link {
		final String labelKey;
		final Supplier<Screen> screen;

		Link(String labelKey, Supplier<Screen> screen) {
			this.labelKey = labelKey;
			this.screen = screen;
		}
	}
}
