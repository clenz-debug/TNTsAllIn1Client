package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5ah, reworked for multi-pin (own user follow-up request): dedicated options screen for the
 * pinned-recipe feature. Two key rebinds (same "click, then press a key; Escape unbinds" pattern
 * as {@link WaypointOptionsScreen}/{@link ZoomOptionsScreen}: one for
 * {@link ModKeyBindings#PIN_RECIPE} (pin/unpin whatever's hovered in the recipe book), one for the
 * own-menu shortcut {@link ModKeyBindings#OPEN_PINNED_RECIPES} - own user follow-up request, "wie
 * beim Waypoint-Menü" - plus a "Rezepte verwalten..." button into {@link PinnedRecipeListScreen}
 * for mouse-only access to the same screen, same "each feature's per-entry management gets its own
 * screen, reachable both ways" convention {@link WaypointOptionsScreen} already established.
 *
 * <p>Three {@link ColorPickerPanel}s (own follow-up request, "die Zahlen sollen wie die Pfeile auch
 * von der Farbe her angepasst werden", then further widened to "auch die Farben bei den Zahlen der
 * Hauptrezepte") - the arrow color already existed; the main ingredient/result count color and the
 * sub-ingredient count color are both new (see {@link ClientConfig#pinnedRecipeCountColor}'s own
 * comment for how the main counts went from vanilla's uncolorable badge to a hand-rolled, colorable
 * one). Three full color pickers plus everything else no longer fit without scrolling, so this now
 * uses the same scroll-with-scissor + {@link ScrollBarHelper} pattern as
 * {@link KeystrokesOptionsScreen} (which hit the same problem first, with its own two color pickers).
 */
public class PinnedRecipeOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int LABEL_HEIGHT = 12;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private @Nullable ColorPickerPanel countColorPicker;
	private @Nullable ColorPickerPanel arrowColorPicker;
	private @Nullable ColorPickerPanel subCountColorPicker;
	private @Nullable ScrollBarHelper scrollBar;
	private @Nullable Button pinRebindButton;
	private @Nullable Button openRebindButton;
	private @Nullable KeyMapping awaitingKeyMapping;
	private int labelX;
	private int countColorLabelY;
	private int arrowColorLabelY;
	private int subCountColorLabelY;
	private int hintY;
	private int scrollOffset;
	private int maxScroll;

	public PinnedRecipeOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.pinned_recipe_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();

		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		int contentHeight = computeContentHeight() - TOP_MARGIN;
		this.maxScroll = Math.max(0, contentHeight - viewportHeight);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

		this.labelX = (this.width - ROW_WIDTH) / 2;
		int y = TOP_MARGIN - this.scrollOffset;

		if (this.scrollBar == null) {
			this.scrollBar = new ScrollBarHelper(this.labelX + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight,
					() -> this.scrollOffset, () -> this.maxScroll,
					newOffset -> {
						this.scrollOffset = newOffset;
						this.rebuild();
					});
		} else {
			this.scrollBar.reposition(this.labelX + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight);
		}

		this.addRenderableWidget(CycleButton.onOffBuilder(config.pinnedRecipeEnabled)
				.create(this.labelX, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.pinned_recipe_options.enabled"),
						(button, value) -> {
							config.pinnedRecipeEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.pinnedRecipeShowSubIngredients)
				.create(this.labelX, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.pinned_recipe_options.show_sub_ingredients"),
						(button, value) -> {
							config.pinnedRecipeShowSubIngredients = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.pinnedRecipeShowFullAmounts)
				.create(this.labelX, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.pinned_recipe_options.show_full_amounts"),
						(button, value) -> {
							config.pinnedRecipeShowFullAmounts = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.countColorLabelY = y;
		y += LABEL_HEIGHT;
		this.countColorPicker = new ColorPickerPanel(this.font, this.labelX, y, ROW_WIDTH, config.pinnedRecipeCountColor,
				this::addRenderableWidget,
				argb -> {
					config.pinnedRecipeCountColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 10;

		this.arrowColorLabelY = y;
		y += LABEL_HEIGHT;
		this.arrowColorPicker = new ColorPickerPanel(this.font, this.labelX, y, ROW_WIDTH, config.pinnedRecipeArrowColor,
				this::addRenderableWidget,
				argb -> {
					config.pinnedRecipeArrowColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 10;

		this.subCountColorLabelY = y;
		y += LABEL_HEIGHT;
		this.subCountColorPicker = new ColorPickerPanel(this.font, this.labelX, y, ROW_WIDTH, config.pinnedRecipeSubIngredientCountColor,
				this::addRenderableWidget,
				argb -> {
					config.pinnedRecipeSubIngredientCountColor = argb;
					config.save();
				});
		y += ColorPickerPanel.totalHeight() + 10;

		this.pinRebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKeyMapping = ModKeyBindings.PIN_RECIPE;
					this.updateRebindButtonLabels();
				})
				.bounds(this.labelX, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.hintY = y + ROW_SPACING;
		y += ROW_SPACING + 16;

		this.openRebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKeyMapping = ModKeyBindings.OPEN_PINNED_RECIPES;
					this.updateRebindButtonLabels();
				})
				.bounds(this.labelX, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabels();
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.pinned_recipe_options.manage_button"),
						button -> this.minecraft.setScreen(new PinnedRecipeListScreen(this)))
				.bounds(this.labelX, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(this.labelX, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Mirrors the y-cursor arithmetic in {@link #init} - keep in sync if that layout ever changes. */
	private static int computeContentHeight() {
		int y = TOP_MARGIN;
		y += ROW_SPACING;
		y += ROW_SPACING;
		y += ROW_SPACING + 6;
		y += LABEL_HEIGHT;
		y += ColorPickerPanel.totalHeight() + 10;
		y += LABEL_HEIGHT;
		y += ColorPickerPanel.totalHeight() + 10;
		y += LABEL_HEIGHT;
		y += ColorPickerPanel.totalHeight() + 10;
		y += ROW_SPACING + 16;
		y += ROW_SPACING;
		y += ROW_SPACING;
		y += ROW_SPACING;
		return y;
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	/** Same scissor fix as {@link CrosshairOptionsScreen#extractRenderState}/{@link KeystrokesOptionsScreen#extractRenderState} -
	 * without it, scrolling pushes a row up into the fixed title text instead of disappearing off
	 * the top. */
	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.text(this.font, Component.translatable("gui.tntsallin1client.pinned_recipe_options.count_color_label"),
				this.labelX, this.countColorLabelY, 0xFFFFFFFF);
		this.countColorPicker.render(guiGraphics, 0xFFFFFFFF);
		guiGraphics.text(this.font, Component.translatable("gui.tntsallin1client.pinned_recipe_options.arrow_color_label"),
				this.labelX, this.arrowColorLabelY, 0xFFFFFFFF);
		this.arrowColorPicker.render(guiGraphics, 0xFFFFFFFF);
		guiGraphics.text(this.font, Component.translatable("gui.tntsallin1client.pinned_recipe_options.sub_count_color_label"),
				this.labelX, this.subCountColorLabelY, 0xFFFFFFFF);
		this.subCountColorPicker.render(guiGraphics, 0xFFFFFFFF);
		guiGraphics.centeredText(this.font, Component.translatable("gui.tntsallin1client.pinned_recipe_options.hint"),
				this.width / 2, this.hintY, 0xFFAAAAAA);
		guiGraphics.disableScissor();

		this.scrollBar.render(guiGraphics);
		guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	private void updateRebindButtonLabels() {
		updateRebindButtonLabel(this.pinRebindButton, ModKeyBindings.PIN_RECIPE, "gui.tntsallin1client.pinned_recipe_options.key");
		updateRebindButtonLabel(this.openRebindButton, ModKeyBindings.OPEN_PINNED_RECIPES, "gui.tntsallin1client.pinned_recipe_options.open_key");
	}

	private void updateRebindButtonLabel(@Nullable Button button, KeyMapping mapping, String labelKey) {
		if (button == null) {
			return;
		}
		Component keyName = mapping.getTranslatedKeyMessage();
		Component label = Component.translatable(labelKey, keyName);
		if (mapping == this.awaitingKeyMapping) {
			label = Component.literal("> ")
					.append(label.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
		}
		button.setMessage(label);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.awaitingKeyMapping != null) {
			this.awaitingKeyMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		if (this.scrollBar.mouseClicked(event)) {
			return true;
		}
		if (this.countColorPicker.mouseClicked(event) || this.arrowColorPicker.mouseClicked(event) || this.subCountColorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		if (this.countColorPicker.mouseDragged(event) || this.arrowColorPicker.mouseDragged(event) || this.subCountColorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean releasedCount = this.countColorPicker.mouseReleased();
		boolean releasedArrow = this.arrowColorPicker.mouseReleased();
		boolean releasedSubCount = this.subCountColorPicker.mouseReleased();
		if (this.scrollBar.mouseReleased() || releasedCount || releasedArrow || releasedSubCount) {
			return true;
		}
		return super.mouseReleased(event);
	}

	/** Same wheel-precedence reasoning as {@link CrosshairOptionsScreen#mouseScrolled}. */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
			return true;
		}
		if (this.maxScroll <= 0) {
			return false;
		}
		int newOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollDeltaY * SCROLL_STEP), 0, this.maxScroll);
		if (newOffset != this.scrollOffset) {
			this.scrollOffset = newOffset;
			this.rebuild();
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKeyMapping != null) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			this.awaitingKeyMapping.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
			finishRebind();
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	private void finishRebind() {
		this.awaitingKeyMapping = null;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.updateRebindButtonLabels();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
