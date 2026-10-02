package com.tntsallin1client.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.tntsallin1client.tour.TourRect;
import com.tntsallin1client.tour.TourTargets;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Phase 5e: ingame mod menu, top level (Minecraft design - the client design shows the same
 * {@link ClientMenuFeatures} as cards instead, see {@link ClientModsCardScreen}). Just the on/off switch per feature.
 * Features with more to configure than a toggle get their own dedicated
 * options screen (e.g. {@link ItemCounterOptionsScreen}), opened via a
 * small button next to that feature's toggle - deliberately not one shared
 * options screen for every feature, which would turn into an unrelated,
 * ever-growing list as more features gain settings. Reachable via the pause
 * menu button ({@link PauseMenuIntegration}) or its own keybind
 * ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 *
 * <p>The row list scrolls (a {@link ContainerObjectSelectionList}, the same
 * base class vanilla's own Controls screen uses for its key bindings) -
 * with 5a-5o's worth of features this no longer fits on screen at every GUI
 * scale, so it needed real scrolling rather than the fixed absolute Y
 * positions this screen used up through 5o.
 */
public class ClientMenuScreen extends Screen implements TourTargets {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int SEARCH_BOX_Y = 30;
	private static final int LIST_TOP = SEARCH_BOX_Y + ROW_HEIGHT + 6;
	private static final int FOOTER_HEIGHT = 30;

	private final @Nullable Screen parent;

	/**
	 * Survives across {@link #init()} reruns because it lives on this Screen instance, not the
	 * {@link EditBox} widget itself - opening a feature's own options screen and clicking back
	 * calls {@code setScreen} on this same instance again (see {@code onClose}/the per-feature
	 * screens' own "back" buttons), which rebuilds every widget from scratch including a brand new,
	 * empty search box. Without this field, that meant re-typing the same search after every single
	 * options-screen visit - not just annoying, actively defeats the point of Phase 5v's search.
	 */
	private String searchQuery = "";

	/** For the in-game tour's highlights ({@link #tourTarget}). */
	private @Nullable FeatureList list;
	private @Nullable EditBox searchBox;

	public ClientMenuScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.tntsallin1client.menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		FeatureList list = new FeatureList(this.minecraft, this.width, listHeight, LIST_TOP);

		ClientMenuFeatures.populate(list, this);

		list.finishBuilding();
		list.filter(this.searchQuery);

		EditBox searchBox = new EditBox(this.font, (this.width - ROW_WIDTH) / 2, SEARCH_BOX_Y, ROW_WIDTH, ROW_HEIGHT,
				Component.translatable("gui.tntsallin1client.menu.search"));
		searchBox.setHint(Component.translatable("gui.tntsallin1client.menu.search"));
		searchBox.setValue(this.searchQuery);
		searchBox.setResponder(value -> {
			this.searchQuery = value;
			list.filter(value);
		});
		this.addRenderableWidget(searchBox);

		this.addRenderableWidget(list);
		this.list = list;
		this.searchBox = searchBox;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds((this.width - ROW_WIDTH) / 2, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public @Nullable TourRect tourTarget(String name) {
		if (this.list == null) return null;
		if (name.equals(SEARCH)) return this.searchBox != null ? TourRect.of(this.searchBox) : null;
		if (name.equals(HUD_EDITOR)) return this.list.rowBounds("gui.tntsallin1client.menu.hud_editor_button", FeatureList.Part.WHOLE);
		if (name.startsWith(FEATURE_OPTIONS)) return this.list.rowBounds(name.substring(FEATURE_OPTIONS.length()), FeatureList.Part.OPTIONS);
		if (name.startsWith(FEATURE_SWITCH)) return this.list.rowBounds(name.substring(FEATURE_SWITCH.length()), FeatureList.Part.SWITCH);
		if (name.startsWith(FEATURE)) return this.list.rowBounds(name.substring(FEATURE.length()), FeatureList.Part.WHOLE);
		return null;
	}

	/** The translation key behind a menu label - identifies rows for the tour. */
	private static String keyOf(Component label) {
		return label.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
	}

	/**
	 * Scrollable row list - {@link ContainerObjectSelectionList}, same base class as vanilla's Controls screen.
	 *
	 * <p>Phase 5w: rows are grouped into {@link Section}s (a header {@link Row} plus its member rows) via
	 * {@link #beginSection(Component)} - explicitly plain in-list headers, not separate screens, per the
	 * idea list's own wording. {@link #filter(String)} rebuilds the visible entries from {@link #sections}
	 * on every keystroke (a section whose rows are all filtered out drops its header too, so searching
	 * never leaves a heading floating above an empty group), the same {@code replaceEntries} mechanism
	 * used before sections existed (Phase 5v).
	 */
	private static class FeatureList extends ContainerObjectSelectionList<FeatureList.Row> implements FeatureSink {
		private final List<Section> sections = new ArrayList<>();
		private @Nullable Section currentSection;

		FeatureList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		@Override
		public void beginSection(Component label) {
			this.currentSection = new Section(Row.header(label, this.minecraft.font));
			this.sections.add(this.currentSection);
		}

		@Override
		public void addToggleRow(boolean initial, Component label, Consumer<Boolean> onToggle,
				@Nullable Supplier<Screen> optionsScreenFactory) {
			boolean hasOptions = optionsScreenFactory != null;
			int toggleWidth = hasOptions ? ROW_WIDTH - OPTIONS_BUTTON_WIDTH - TOGGLE_GAP : ROW_WIDTH;
			CycleButton<Boolean> toggle = CycleButton.onOffBuilder(initial)
					.create(0, 0, toggleWidth, ROW_HEIGHT, label, (button, value) -> onToggle.accept(value));
			Button options = hasOptions
					? Button.builder(Component.translatable("gui.tntsallin1client.menu.options_button"),
									button -> Minecraft.getInstance().gui.setScreen(optionsScreenFactory.get()))
							.bounds(0, 0, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT)
							.build()
					: null;
			this.currentSection.rows.add(new Row(toggle, options, label.getString(), keyOf(label)));
		}

		@Override
		public void addButtonRow(ButtonRole role, Component label, Runnable onPress) {
			Button button = Button.builder(label, b -> onPress.run()).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
			this.currentSection.rows.add(new Row(button, null, label.getString(), keyOf(label)));
		}

		/** Populates the list for the first time - call once after the last {@code add*Row}/{@code beginSection}. */
		void finishBuilding() {
			filter("");
		}

		/** Empty query shows every row again; otherwise a case-insensitive substring match per row, section by section. */
		void filter(String query) {
			String needle = query.strip().toLowerCase(Locale.ROOT);
			List<Row> visible = new ArrayList<>();
			for (Section section : this.sections) {
				List<Row> matches = needle.isEmpty()
						? section.rows
						: section.rows.stream().filter(row -> row.searchKey.contains(needle)).toList();
				if (!matches.isEmpty()) {
					visible.add(section.header);
					visible.addAll(matches);
				}
			}
			this.replaceEntries(visible);
		}

		enum Part {
			WHOLE,
			SWITCH,
			OPTIONS
		}

		/**
		 * Where a visible row (by its label's translation key) is, for the in-game tour. A row outside
		 * the list's viewport gets scrolled into view instead - it's there from the next frame on.
		 */
		@Nullable TourRect rowBounds(String key, Part part) {
			for (Row row : this.children()) {
				if (row.header || !row.key.equals(key)) continue;
				int top = row.getContentY();
				if (top < this.getY() || top + ROW_HEIGHT > this.getY() + this.getHeight()) {
					this.scrollToEntry(row);
					return null;
				}
				int left = row.getContentX();
				return switch (part) {
					case WHOLE -> new TourRect(left, top, ROW_WIDTH, ROW_HEIGHT);
					case SWITCH -> new TourRect(left, top, row.primary.getWidth(), ROW_HEIGHT);
					case OPTIONS -> row.secondary == null ? null
							: new TourRect(left + row.primary.getWidth() + TOGGLE_GAP, top, row.secondary.getWidth(), ROW_HEIGHT);
				};
			}
			return null;
		}

		/** A section header plus the rows added while it was the current section. */
		private static final class Section {
			final Row header;
			final List<Row> rows = new ArrayList<>();

			Section(Row header) {
				this.header = header;
			}
		}

		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final AbstractWidget primary;
			private final @Nullable AbstractWidget secondary;
			private final String searchKey;
			/** The label's translation key - how the in-game tour finds a row. */
			private final String key;
			private final boolean header;

			Row(AbstractWidget primary, @Nullable AbstractWidget secondary, String label, String key) {
				this.primary = primary;
				this.secondary = secondary;
				this.searchKey = label.toLowerCase(Locale.ROOT);
				this.key = key;
				this.header = false;
			}

			private Row(AbstractWidget primary, String label) {
				this.primary = primary;
				this.secondary = null;
				this.searchKey = label.toLowerCase(Locale.ROOT);
				this.key = "";
				this.header = true;
			}

			/** Centered, bottom-aligned label with no click behavior - same look as vanilla's Controls-screen categories. */
			static Row header(Component label, net.minecraft.client.gui.Font font) {
				FocusableTextWidget widget = FocusableTextWidget.builder(label, font)
						.alwaysShowBorder(false)
						.backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
						.build();
				return new Row(widget, label.getString());
			}

			@Override
			public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				if (this.header) {
					this.primary.setPosition(this.getContentX() + (ROW_WIDTH - this.primary.getWidth()) / 2,
							this.getContentBottom() - this.primary.getHeight());
				} else {
					this.primary.setPosition(this.getContentX(), this.getContentY());
				}
				this.primary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
				if (this.secondary != null) {
					this.secondary.setPosition(this.getContentX() + this.primary.getWidth() + TOGGLE_GAP, this.getContentY());
					this.secondary.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
				}
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return this.secondary == null ? List.of(this.primary) : List.of(this.primary, this.secondary);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return this.secondary == null ? List.of(this.primary) : List.of(this.primary, this.secondary);
			}
		}
	}
}
