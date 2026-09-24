package com.tntsallin1client.menu;

import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.FeatureIcons;
import com.tntsallin1client.design.ThemedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Client Mods menu in the client design (own user sketch): Done / Search / Move-Resize along
 * the top, then every feature as a card - a symbol with the feature's name under it on top, an
 * Optionen button in the middle that opens the feature's options screen (greyed out when it has
 * none), and an Aktiviert/Deaktiviert bar at the bottom that toggles it. Same entries as the
 * Minecraft-design list ({@link ClientMenuFeatures}), grouped by the same sections.
 *
 * <p>The symbols come from {@link FeatureIcons}; features without one show their name large and
 * centered in the image area instead.
 */
public class ClientModsCardScreen extends Screen implements FeatureSink {
	private static final int MARGIN = 10;
	private static final int TOP_BAR_Y = 8;
	private static final int TOP_BAR_HEIGHT = 20;
	private static final int CONTENT_TOP = TOP_BAR_Y + TOP_BAR_HEIGHT + 10;
	private static final int FOOTER_HEIGHT = 30;
	private static final int CARD_WIDTH = 110;
	private static final int CARD_GAP = 10;
	private static final int IMAGE_HEIGHT = 48;
	private static final int ICON_SIZE = 28;
	private static final int ICON_TOP = 4;
	/** Scales tried for the name on cards without a symbol - the first one it fits at wins. */
	private static final float[] LARGE_NAME_SCALES = {1.5f, 1.0f};
	private static final int OPTIONS_HEIGHT = 14;
	private static final int STATUS_HEIGHT = 16;
	private static final int CARD_HEIGHT = IMAGE_HEIGHT + OPTIONS_HEIGHT + STATUS_HEIGHT;
	private static final int SECTION_HEADER_HEIGHT = 16;
	private static final int SCROLL_STEP = 20;
	private static final int SCROLLBAR_WIDTH = 4;

	private final @Nullable Screen parent;
	private final List<Section> sections = new ArrayList<>();
	private final List<Footer> footerButtons = new ArrayList<>();
	private @Nullable Runnable hudEditor;

	/** Kept across {@link #init()} reruns, like {@link ClientMenuScreen}'s own search - coming back from an options screen rebuilds everything. */
	private String searchQuery = "";
	private int scrollOffset;

	public ClientModsCardScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.tntsallin1client.menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.sections.clear();
		this.footerButtons.clear();
		ClientMenuFeatures.populate(this, this);

		int searchWidth = Math.min(200, this.width - 2 * MARGIN - 2 * 80 - 2 * CARD_GAP);
		this.addRenderableWidget(new ThemedButton(MARGIN, TOP_BAR_Y, 80, TOP_BAR_HEIGHT, CommonComponents.GUI_DONE, this::onClose));

		EditBox search = new EditBox(this.font, (this.width - searchWidth) / 2, TOP_BAR_Y, searchWidth, TOP_BAR_HEIGHT,
				Component.translatable("gui.tntsallin1client.menu.search"));
		search.setHint(Component.translatable("gui.tntsallin1client.menu.search"));
		search.setValue(this.searchQuery);
		search.setResponder(value -> {
			this.searchQuery = value;
			this.scrollOffset = 0;
		});
		this.addRenderableWidget(search);

		if (this.hudEditor != null) {
			Runnable openHudEditor = this.hudEditor;
			this.addRenderableWidget(new ThemedButton(this.width - MARGIN - 80, TOP_BAR_Y, 80, TOP_BAR_HEIGHT,
					Component.translatable("gui.tntsallin1client.cards.move_resize"), openHudEditor));
		}

		int footerWidth = 120;
		int footerX = (this.width - (this.footerButtons.size() * (footerWidth + CARD_GAP) - CARD_GAP)) / 2;
		for (Footer footer : this.footerButtons) {
			this.addRenderableWidget(new ThemedButton(footerX, this.height - FOOTER_HEIGHT + 5, footerWidth, TOP_BAR_HEIGHT, footer.label, footer.onPress));
			footerX += footerWidth + CARD_GAP;
		}
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());
	}

	// --- FeatureSink --------------------------------------------------------------------------

	@Override
	public void beginSection(Component label) {
		this.sections.add(new Section(label));
	}

	@Override
	public void addToggleRow(boolean initial, Component label, Consumer<Boolean> onToggle, @Nullable Supplier<Screen> optionsScreenFactory) {
		this.sections.getLast().cards.add(new Card(label, initial, onToggle, optionsScreenFactory));
	}

	@Override
	public void addButtonRow(ButtonRole role, Component label, Runnable onPress) {
		if (role == ButtonRole.HUD_EDITOR) {
			this.hudEditor = onPress;
		} else {
			this.footerButtons.add(new Footer(label, onPress));
		}
	}

	// --- Layout ------------------------------------------------------------------------------

	private int columns() {
		return Math.max(1, (this.width - 2 * MARGIN + CARD_GAP) / (CARD_WIDTH + CARD_GAP));
	}

	private int gridLeft() {
		int columns = columns();
		return (this.width - (columns * (CARD_WIDTH + CARD_GAP) - CARD_GAP)) / 2;
	}

	private int viewportBottom() {
		return this.height - FOOTER_HEIGHT;
	}

	/** Visible sections for the current search - a section with no matching card is left out entirely, header included. */
	private List<Section> visibleSections() {
		String needle = this.searchQuery.strip().toLowerCase(Locale.ROOT);
		List<Section> visible = new ArrayList<>();
		for (Section section : this.sections) {
			List<Card> matches = needle.isEmpty()
					? section.cards
					: section.cards.stream().filter(card -> card.searchKey.contains(needle)).toList();
			if (!matches.isEmpty()) {
				Section filtered = new Section(section.label);
				filtered.cards.addAll(matches);
				visible.add(filtered);
			}
		}
		return visible;
	}

	/** Positions every visible card (unscrolled content coordinates) and returns the total content height. */
	private int layout(List<Section> visible, List<PlacedCard> placed, List<PlacedHeader> headers) {
		int columns = columns();
		int left = gridLeft();
		int y = 0;
		for (Section section : visible) {
			headers.add(new PlacedHeader(section.label, y));
			y += SECTION_HEADER_HEIGHT;
			for (int i = 0; i < section.cards.size(); i++) {
				int column = i % columns;
				int row = i / columns;
				placed.add(new PlacedCard(section.cards.get(i), left + column * (CARD_WIDTH + CARD_GAP), y + row * (CARD_HEIGHT + CARD_GAP)));
			}
			int rows = (section.cards.size() + columns - 1) / columns;
			y += rows * (CARD_HEIGHT + CARD_GAP);
		}
		return y;
	}

	private int maxScroll() {
		int contentHeight = layout(visibleSections(), new ArrayList<>(), new ArrayList<>());
		return Math.max(0, contentHeight - (viewportBottom() - CONTENT_TOP));
	}

	// --- Rendering ---------------------------------------------------------------------------

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		graphics.fill(0, 0, this.width, this.height, ClientTheme.get().background1);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
		ClientTheme theme = ClientTheme.get();
		List<PlacedCard> placed = new ArrayList<>();
		List<PlacedHeader> headers = new ArrayList<>();
		int contentHeight = layout(visibleSections(), placed, headers);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, Math.max(0, contentHeight - (viewportBottom() - CONTENT_TOP)));
		int top = CONTENT_TOP - this.scrollOffset;

		graphics.enableScissor(0, CONTENT_TOP, this.width, viewportBottom());
		for (PlacedHeader header : headers) {
			graphics.drawString(this.font, ClientFont.of(header.label), gridLeft(), top + header.y + 4, theme.text, false);
		}
		for (PlacedCard card : placed) {
			boolean hovered = mouseY >= CONTENT_TOP && mouseY < viewportBottom()
					&& mouseX >= card.x && mouseX < card.x + CARD_WIDTH && mouseY >= top + card.y && mouseY < top + card.y + CARD_HEIGHT;
			renderCard(graphics, card.card, card.x, top + card.y, hovered ? mouseY - (top + card.y) : -1);
		}
		graphics.disableScissor();

		int maxScroll = Math.max(0, contentHeight - (viewportBottom() - CONTENT_TOP));
		if (maxScroll > 0) {
			int viewport = viewportBottom() - CONTENT_TOP;
			int thumb = Math.max(20, viewport * viewport / contentHeight);
			int thumbY = CONTENT_TOP + this.scrollOffset * (viewport - thumb) / maxScroll;
			int x = this.width - MARGIN / 2 - SCROLLBAR_WIDTH;
			graphics.fill(x, CONTENT_TOP, x + SCROLLBAR_WIDTH, viewportBottom(), theme.background2);
			graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, theme.accent3);
		}

		super.render(graphics, mouseX, mouseY, a);
	}

	/** {@code hoverY} is the mouse's y inside the card, or -1 when it isn't over it. */
	private void renderCard(GuiGraphics graphics, Card card, int x, int y, int hoverY) {
		ClientTheme theme = ClientTheme.get();
		boolean optionsHovered = hoverY >= IMAGE_HEIGHT && hoverY < IMAGE_HEIGHT + OPTIONS_HEIGHT && card.options != null;
		boolean statusHovered = hoverY >= IMAGE_HEIGHT + OPTIONS_HEIGHT;

		// Image area - the symbol with the name under it, or just the name large for cards without one
		graphics.fill(x, y, x + CARD_WIDTH, y + IMAGE_HEIGHT, theme.background1);
		if (!FeatureIcons.has(card.key)) {
			renderLargeName(graphics, card.label.getString(), x, y);
		} else {
			FeatureIcons.draw(graphics, this.font, card.key, x + (CARD_WIDTH - ICON_SIZE) / 2, y + ICON_TOP, ICON_SIZE);
			Component title = ClientFont.fit(this.font, card.label.getString(), CARD_WIDTH - 6);
			int titleY = y + ICON_TOP + ICON_SIZE + (IMAGE_HEIGHT - ICON_TOP - ICON_SIZE - this.font.lineHeight) / 2;
			graphics.drawString(this.font, title, x + (CARD_WIDTH - this.font.width(title)) / 2, titleY, theme.text, false);
		}

		// Options button - greyed out for features without an options screen
		int optionsTop = y + IMAGE_HEIGHT;
		graphics.fill(x, optionsTop, x + CARD_WIDTH, optionsTop + OPTIONS_HEIGHT, optionsHovered ? theme.accent1 : theme.background2);
		Component options = ClientFont.of(Component.translatable("gui.tntsallin1client.cards.options"));
		float optionsAlpha = card.options == null ? 0.3f : optionsHovered ? 1.0f : 0.8f;
		graphics.drawString(this.font, options, x + (CARD_WIDTH - this.font.width(options)) / 2, optionsTop + (OPTIONS_HEIGHT - this.font.lineHeight) / 2,
				ClientTheme.withAlpha(theme.text, optionsAlpha), false);

		// Status bar
		int statusTop = optionsTop + OPTIONS_HEIGHT;
		int statusFill = card.enabled ? (statusHovered ? theme.accent4 : theme.accent3) : (statusHovered ? theme.accent1 : theme.background2);
		graphics.fill(x, statusTop, x + CARD_WIDTH, statusTop + STATUS_HEIGHT, statusFill);
		Component status = ClientFont.of(Component.translatable(card.enabled ? "gui.tntsallin1client.cards.enabled" : "gui.tntsallin1client.cards.disabled"));
		graphics.drawString(this.font, status, x + (CARD_WIDTH - this.font.width(status)) / 2, statusTop + (STATUS_HEIGHT - this.font.lineHeight) / 2,
				ClientTheme.withAlpha(theme.text, card.enabled ? 1.0f : 0.6f), false);

		// Frame and separators, like the sketch's boxes
		int frame = hoverY >= 0 ? theme.accent4 : theme.accent2;
		graphics.renderOutline(x, y, CARD_WIDTH, CARD_HEIGHT, frame);
		graphics.fill(x, optionsTop, x + CARD_WIDTH, optionsTop + 1, frame);
		graphics.fill(x, statusTop, x + CARD_WIDTH, statusTop + 1, frame);
	}

	/**
	 * The name word-wrapped (at spaces and after hyphens, so "Inventar-Schnellsortierung" breaks too)
	 * and centered in the image area - at the largest of {@link #LARGE_NAME_SCALES} it fits at.
	 */
	private void renderLargeName(GuiGraphics graphics, String name, int x, int y) {
		float scale = LARGE_NAME_SCALES[LARGE_NAME_SCALES.length - 1];
		List<String> lines = List.of();
		for (float candidate : LARGE_NAME_SCALES) {
			lines = wrapName(name, (CARD_WIDTH - 8) / candidate);
			boolean fits = lines.size() * this.font.lineHeight * candidate <= IMAGE_HEIGHT - 4
					&& lines.stream().allMatch(line -> this.font.width(ClientFont.of(line)) <= (CARD_WIDTH - 8) / candidate);
			scale = candidate;
			if (fits) {
				break;
			}
		}
		float maxWidth = (CARD_WIDTH - 8) / scale;
		float lineHeight = this.font.lineHeight * scale;
		float top = y + (IMAGE_HEIGHT - lines.size() * lineHeight) / 2 + 1;
		for (int i = 0; i < lines.size(); i++) {
			Component line = ClientFont.fit(this.font, lines.get(i), (int) maxWidth);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + CARD_WIDTH / 2.0f, top + i * lineHeight);
			graphics.pose().scale(scale, scale);
			graphics.drawString(this.font, line, -this.font.width(line) / 2, 0, ClientTheme.get().text, false);
			graphics.pose().popMatrix();
		}
	}

	private List<String> wrapName(String name, float maxWidth) {
		List<String> lines = new ArrayList<>();
		// Pieces keep their leading space / trailing hyphen, so lines are just concatenations
		for (String piece : name.split("(?<=-)|(?= )")) {
			int last = lines.size() - 1;
			if (last >= 0 && this.font.width(ClientFont.of(lines.get(last) + piece)) <= maxWidth) {
				lines.set(last, lines.get(last) + piece);
			} else {
				lines.add(piece.strip());
			}
		}
		return lines;
	}

	// --- Input -------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0 || event.y() < CONTENT_TOP || event.y() >= viewportBottom()) {
			return false;
		}
		List<PlacedCard> placed = new ArrayList<>();
		layout(visibleSections(), placed, new ArrayList<>());
		int top = CONTENT_TOP - this.scrollOffset;
		for (PlacedCard card : placed) {
			double localX = event.x() - card.x;
			double localY = event.y() - (top + card.y);
			if (localX < 0 || localX >= CARD_WIDTH || localY < 0 || localY >= CARD_HEIGHT) {
				continue;
			}
			if (localY >= IMAGE_HEIGHT + OPTIONS_HEIGHT) {
				card.card.enabled = !card.card.enabled;
				card.card.onToggle.accept(card.card.enabled);
				playClickSound();
				return true;
			}
			if (localY >= IMAGE_HEIGHT && card.card.options != null) {
				playClickSound();
				this.minecraft.setScreen(card.card.options.get());
				return true;
			}
			return false;
		}
		return false;
	}

	private void playClickSound() {
		AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), 0, maxScroll());
		return true;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	// --- Data --------------------------------------------------------------------------------

	private static final class Section {
		final Component label;
		final List<Card> cards = new ArrayList<>();

		Section(Component label) {
			this.label = label;
		}
	}

	private static final class Card {
		final Component label;
		/** The label's translation key - picks the card's symbol in {@link FeatureIcons}. */
		final String key;
		final String searchKey;
		final Consumer<Boolean> onToggle;
		final @Nullable Supplier<Screen> options;
		boolean enabled;

		Card(Component label, boolean enabled, Consumer<Boolean> onToggle, @Nullable Supplier<Screen> options) {
			this.label = label;
			this.key = label.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
			this.searchKey = label.getString().toLowerCase(Locale.ROOT);
			this.enabled = enabled;
			this.onToggle = onToggle;
			this.options = options;
		}
	}

	private record Footer(Component label, Runnable onPress) {
	}

	private record PlacedCard(Card card, int x, int y) {
	}

	private record PlacedHeader(Component label, int y) {
	}
}
