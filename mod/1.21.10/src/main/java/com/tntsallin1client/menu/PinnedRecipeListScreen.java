package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.recipe.PinnedRecipe;
import com.tntsallin1client.recipe.PinnedRecipeHud;
import com.tntsallin1client.recipe.PinnedRecipeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Own user request ("vll wäre ein Rezept-Menü cool wie quasi das Waypoint-Menü") - list of every
 * currently pinned recipe (up to {@link PinnedRecipeManager#MAX_PINNED}), same
 * {@link ContainerObjectSelectionList} base + per-row visibility toggle shape as
 * {@link WaypointListScreen}. Unlike waypoints there's no per-entry edit screen (a pinned recipe
 * has nothing to rename/recolor/reposition) and no "new" button (pins are only ever created from
 * the recipe book itself via {@link PinnedRecipeManager}'s keybind) - each row is a label (the
 * result item, from {@link PinnedRecipeHud#resultStack}), a craft-quantity control (+/- buttons
 * plus a typeable field), the visibility toggle, and a compact remove button. No per-row delete
 * confirmation (unlike waypoints) - re-pinning a removed recipe is a single keypress away in the
 * recipe book, so the extra friction isn't worth it here; the bulk "remove all" button below the
 * list does confirm, same reasoning {@link WaypointListScreen#confirmDeleteAll} already uses for
 * its own bulk delete.
 *
 * <p>The visibility toggle refuses to turn a recipe on once {@link PinnedRecipeManager#MAX_PINNED}
 * others are already visible ({@link #setVisible}) - own follow-up request, recipes pinned past
 * that cap "sollen nicht angezeigt werden können", not just default to hidden. Rejecting a toggle
 * rebuilds the screen (same {@code setScreen} trick {@link #remove} uses) to snap the button back
 * to "Aus" instead of leaving it displaying a state the config never actually took.
 *
 * <p>The craft-quantity control adjusts {@link PinnedRecipe#craftMultiplier} - own follow-up
 * request ("die Menge an Items die man vom gepinnten Rezept craften will anzeigen können") - which
 * scales every ingredient/result count this recipe shows, both here (via {@link #rowLabel}) and on
 * the HUD. The +/- buttons ({@link #changeCraftMultiplier}) rebuild the whole screen on every click,
 * same as every other change in this screen, since the row's label {@code Component} is otherwise
 * fixed at construction time. The quantity {@link EditBox} deliberately does *not* rebuild on every
 * keystroke ({@link #setCraftMultiplierText}) - own further follow-up request ("auch über ein Feld
 * eintippbar machen, wenn man z.B. auch mehr als 64 Items braucht", the +/- stepper alone originally
 * capped at 64) - rebuilding mid-edit would tear down and recreate the very box the player is typing
 * into, dropping keyboard focus after every character; the row's label simply goes one edit stale
 * until the screen is next reopened or a +/- click rebuilds it anyway.
 */
public class PinnedRecipeListScreen extends Screen {
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 24;
	private static final int QTY_BUTTON_WIDTH = 16;
	private static final int QTY_EDIT_BOX_WIDTH = 32;
	private static final int VISIBLE_TOGGLE_WIDTH = 50;
	private static final int REMOVE_BUTTON_WIDTH = 20;
	private static final int WIDGET_GAP = 4;
	private static final int LIST_TOP = 30;
	private static final int FOOTER_HEIGHT = 30;

	private final Screen parent;

	public PinnedRecipeListScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.pinned_recipe_list.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		List<PinnedRecipe> pinned = ClientConfig.get().pinnedRecipes;

		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT - ROW_HEIGHT - 6;
		RecipeList list = new RecipeList(this.minecraft.font, this.width, listHeight, LIST_TOP, pinned, this);
		this.addRenderableWidget(list);

		int buttonX = (this.width - ROW_WIDTH) / 2;
		Button removeAllButton = Button.builder(Component.translatable("gui.tntsallin1client.pinned_recipe_list.delete_all_button"),
						button -> this.confirmRemoveAll())
				.bounds(buttonX, LIST_TOP + listHeight + 6, ROW_WIDTH, ROW_HEIGHT)
				.build();
		removeAllButton.active = !pinned.isEmpty();
		this.addRenderableWidget(removeAllButton);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(buttonX, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private void remove(PinnedRecipe recipe) {
		ClientConfig config = ClientConfig.get();
		config.pinnedRecipes.remove(recipe);
		config.save();
		// setScreen on this same instance re-runs init() regardless (see ClientMenuScreen's own doc
		// comment on the same behavior) - rebuilds the list from the now-shortened config either way.
		this.minecraft.setScreen(this);
	}

	/** Always confirms, regardless of any single-recipe removal not needing one (see this class's own
	 * doc comment) - a bulk, more-tedious-to-redo action, same reasoning
	 * {@link WaypointListScreen#confirmDeleteAll} already uses for its own bulk delete. */
	private void confirmRemoveAll() {
		int count = ClientConfig.get().pinnedRecipes.size();
		if (count == 0) {
			return;
		}
		this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				ClientConfig.get().pinnedRecipes.clear();
				ClientConfig.get().save();
			}
			this.minecraft.setScreen(this);
		}, Component.translatable("gui.tntsallin1client.pinned_recipe_list.delete_all_confirm_title"),
				Component.translatable("gui.tntsallin1client.pinned_recipe_list.delete_all_confirm_message", count)));
	}

	private void setVisible(PinnedRecipe recipe, boolean value) {
		ClientConfig config = ClientConfig.get();
		if (value && PinnedRecipeManager.visibleCount(config.pinnedRecipes) >= PinnedRecipeManager.MAX_PINNED) {
			// Already at the HUD display cap - see this class's own doc comment. Rebuild so the
			// CycleButton is recreated from the (still-false) config value instead of staying stuck
			// showing "An" for a change that never actually applied.
			this.minecraft.setScreen(this);
			return;
		}
		recipe.visible = value;
		config.save();
	}

	private void changeCraftMultiplier(PinnedRecipe recipe, int delta) {
		int next = Mth.clamp(recipe.craftMultiplier + delta, 1, PinnedRecipe.MAX_CRAFT_MULTIPLIER);
		if (next == recipe.craftMultiplier) {
			return;
		}
		recipe.craftMultiplier = next;
		ClientConfig.get().save();
		// Rebuilds so the row label (which bakes craftMultiplier into its item count via
		// PinnedRecipeHud#resultStack) and the quantity field's own text both reflect the new value.
		this.minecraft.setScreen(this);
	}

	/** Own follow-up request: the quantity field's own text, typed directly rather than via +/-
	 * clicks - see this class's own doc comment for why this deliberately never rebuilds the screen.
	 * {@link EditBox#setFilter} already keeps {@code text} digits-only before this ever runs. */
	private void setCraftMultiplierText(PinnedRecipe recipe, String text) {
		if (text.isEmpty()) {
			// Momentary state while the player selects-and-retypes the whole field - keep the last
			// valid value rather than snapping to some fallback out from under them mid-edit.
			return;
		}
		try {
			int typed = Integer.parseInt(text);
			if (typed >= 1) {
				recipe.craftMultiplier = Math.min(typed, PinnedRecipe.MAX_CRAFT_MULTIPLIER);
				ClientConfig.get().save();
			}
		} catch (NumberFormatException ignored) {
			// Digits-only filter already prevents this in practice; falling back to "ignore" rather
			// than throwing keeps typing robust regardless.
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		if (ClientConfig.get().pinnedRecipes.isEmpty()) {
			MenuText.centered(guiGraphics, this.font, Component.translatable("gui.tntsallin1client.pinned_recipe_list.empty"),
					this.width / 2, LIST_TOP + 10, 0xFFAAAAAA);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	/** Result-item label, e.g. "3x Redstone-Wiederholer" - same source {@link PinnedRecipeHud}'s own
	 * render/HUD-editor-bounds use (already scaled by {@link PinnedRecipe#craftMultiplier}), so a row
	 * here always matches what the HUD would actually show. */
	static Component rowLabel(PinnedRecipe recipe) {
		ItemStack resultStack = PinnedRecipeHud.resultStack(recipe);
		if (resultStack.isEmpty()) {
			return Component.translatable("gui.tntsallin1client.pinned_recipe_list.unknown_item");
		}
		return Component.literal(resultStack.getCount() + "x ").append(resultStack.getHoverName());
	}

	private static final class RecipeList extends ContainerObjectSelectionList<RecipeList.Row> {
		RecipeList(Font font, int width, int height, int y, List<PinnedRecipe> pinned, PinnedRecipeListScreen owner) {
			super(owner.minecraft, width, height, y, ITEM_HEIGHT);
			for (PinnedRecipe recipe : pinned) {
				this.addEntry(Row.of(font, recipe, owner));
			}
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final Font font;
			/** Recomputed fresh every {@link #renderContent} call instead of cached at construction -
			 * unlike every other per-row widget here, this has to track the quantity field's live,
			 * uncommitted-to-rebuild typing (see this screen's own doc comment on
			 * {@link #setCraftMultiplierText}). */
			private final PinnedRecipe recipe;
			private final AbstractWidget minusButton;
			private final EditBox qtyBox;
			private final AbstractWidget plusButton;
			private final AbstractWidget visibleToggle;
			private final AbstractWidget removeButton;

			private Row(Font font, PinnedRecipe recipe, AbstractWidget minusButton, EditBox qtyBox, AbstractWidget plusButton,
					AbstractWidget visibleToggle, AbstractWidget removeButton) {
				this.font = font;
				this.recipe = recipe;
				this.minusButton = minusButton;
				this.qtyBox = qtyBox;
				this.plusButton = plusButton;
				this.visibleToggle = visibleToggle;
				this.removeButton = removeButton;
			}

			static Row of(Font font, PinnedRecipe recipe, PinnedRecipeListScreen owner) {
				Button minusButton = Button.builder(Component.literal("-"), button -> owner.changeCraftMultiplier(recipe, -1))
						.bounds(0, 0, QTY_BUTTON_WIDTH, ROW_HEIGHT)
						.build();
				minusButton.active = recipe.craftMultiplier > 1;
				Button plusButton = Button.builder(Component.literal("+"), button -> owner.changeCraftMultiplier(recipe, 1))
						.bounds(0, 0, QTY_BUTTON_WIDTH, ROW_HEIGHT)
						.build();
				plusButton.active = recipe.craftMultiplier < PinnedRecipe.MAX_CRAFT_MULTIPLIER;

				EditBox qtyBox = new EditBox(font, 0, 0, QTY_EDIT_BOX_WIDTH, ROW_HEIGHT,
						Component.translatable("gui.tntsallin1client.pinned_recipe_list.qty"));
				qtyBox.setMaxLength(String.valueOf(PinnedRecipe.MAX_CRAFT_MULTIPLIER).length());
				qtyBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
				qtyBox.setValue(String.valueOf(recipe.craftMultiplier));
				qtyBox.setResponder(text -> owner.setCraftMultiplierText(recipe, text));

				// displayOnlyValue() - same fix as WaypointListScreen's own visible toggle - an empty
				// name Component through the default NAME_AND_VALUE display state renders vanilla's
				// "%s: %s" template with a blank first half, i.e. a stray leading ": " before "An"/"Aus".
				CycleButton<Boolean> visibleToggle = CycleButton.onOffBuilder(recipe.visible)
						.displayOnlyValue()
						.create(0, 0, VISIBLE_TOGGLE_WIDTH, ROW_HEIGHT, Component.empty(),
								(button, value) -> owner.setVisible(recipe, value));
				Button removeButton = Button.builder(Component.literal("X"), button -> owner.remove(recipe))
						.bounds(0, 0, REMOVE_BUTTON_WIDTH, ROW_HEIGHT)
						.build();
				return new Row(font, recipe, minusButton, qtyBox, plusButton, visibleToggle, removeButton);
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				int qtyGroupWidth = QTY_BUTTON_WIDTH * 2 + QTY_EDIT_BOX_WIDTH;
				int labelWidth = ROW_WIDTH - qtyGroupWidth - WIDGET_GAP - VISIBLE_TOGGLE_WIDTH - WIDGET_GAP - REMOVE_BUTTON_WIDTH - WIDGET_GAP;
				int labelY = this.getContentY() + (ROW_HEIGHT - this.font.lineHeight) / 2;
				MenuText.text(guiGraphics, this.font, rowLabel(this.recipe), this.getContentX(), labelY, 0xFFFFFFFF);

				int minusX = this.getContentX() + labelWidth + WIDGET_GAP;
				this.minusButton.setPosition(minusX, this.getContentY());
				this.minusButton.render(guiGraphics, mouseX, mouseY, partialTick);

				int qtyBoxX = minusX + QTY_BUTTON_WIDTH;
				this.qtyBox.setPosition(qtyBoxX, this.getContentY());
				this.qtyBox.render(guiGraphics, mouseX, mouseY, partialTick);

				int plusX = qtyBoxX + QTY_EDIT_BOX_WIDTH;
				this.plusButton.setPosition(plusX, this.getContentY());
				this.plusButton.render(guiGraphics, mouseX, mouseY, partialTick);

				int toggleX = plusX + QTY_BUTTON_WIDTH + WIDGET_GAP;
				this.visibleToggle.setPosition(toggleX, this.getContentY());
				this.visibleToggle.render(guiGraphics, mouseX, mouseY, partialTick);

				int removeX = toggleX + VISIBLE_TOGGLE_WIDTH + WIDGET_GAP;
				this.removeButton.setPosition(removeX, this.getContentY());
				this.removeButton.render(guiGraphics, mouseX, mouseY, partialTick);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.minusButton, this.qtyBox, this.plusButton, this.visibleToggle, this.removeButton);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.minusButton, this.qtyBox, this.plusButton, this.visibleToggle, this.removeButton);
			}
		}
	}
}
