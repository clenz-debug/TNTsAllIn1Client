package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.recipe.PinnedRecipe;
import com.tntsallin1client.recipe.PinnedRecipeHud;
import com.tntsallin1client.recipe.PinnedRecipeManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The list of pinned recipes, reached from the pinned recipe options ("Manage Recipes...") or
 * straight from gameplay by its own key - the same rows as in the newer versions of the mod: the
 * result's name with how many of it, how often to craft it (minus and plus buttons around a field
 * to type the number into), a switch for showing it on the HUD, and a small button that unpins it.
 * Below the list "Remove All", which asks first; unpinning one recipe does not - pinning it again is
 * one key press in the recipe book.
 *
 * <p>The switch refuses to show a recipe while {@link PinnedRecipeManager#MAX_PINNED} others are
 * showing already.
 */
public class PinnedRecipeListScreen extends ClientScreen {
	private static final String KEY = "gui.tntsallin1client.pinned_recipe_list.";
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int QTY_BUTTON_WIDTH = 16;
	private static final int QTY_FIELD_WIDTH = 32;
	private static final int VISIBLE_TOGGLE_WIDTH = 50;
	private static final int REMOVE_BUTTON_WIDTH = 20;
	private static final int WIDGET_GAP = 4;
	private static final int LIST_TOP = 30;
	private static final int MESSAGE_COLOR = 0xAAAAAA;
	private static final int LEFT_MOUSE_BUTTON = 0;

	private final List<Row> rows = new ArrayList<Row>();
	private final ScrollPane pane = new ScrollPane(this::placeRows);

	public PinnedRecipeListScreen(Screen parent) {
		super(parent, KEY + "title");
	}

	/** Runs again after every change that needs the rows built anew - the list is rebuilt from what is pinned now. */
	@Override
	protected void init() {
		List<PinnedRecipe> pinned = ClientConfig.get().pinnedRecipes;
		// Holding a key (backspace) repeats it while this screen is open.
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

		int x = (this.width - ROW_WIDTH) / 2;
		int backY = this.height - 28;
		int removeAllY = backY - ROW_SPACING;

		this.rows.clear();
		int y = 0;
		for (PinnedRecipe recipe : pinned) {
			this.rows.add(new Row(recipe, x, y));
			y += ROW_SPACING;
		}
		int contentHeight = this.rows.isEmpty() ? 0 : y - ROW_SPACING + ROW_HEIGHT;
		this.pane.layout(LIST_TOP, removeAllY - 6, x + ROW_WIDTH + ScrollPane.SCROLLBAR_GAP, contentHeight);

		Button removeAll = this.addButton(new Button(x, removeAllY, ROW_WIDTH, ROW_HEIGHT, I18n.get(KEY + "delete_all_button"), pressed -> askRemoveAll()));
		removeAll.active = !pinned.isEmpty();
		// In the client design "Back" is in the top left corner, like on the options screens.
		this.addButton(TopBar.inUse()
				? TopBar.back(I18n.get("gui.back"), this::back)
				: new Button(x, backY, ROW_WIDTH, ROW_HEIGHT, I18n.get("gui.back"), pressed -> back()));
	}

	@Override
	public void removed() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
	}

	private void askRemoveAll() {
		final int count = ClientConfig.get().pinnedRecipes.size();
		this.minecraft.setScreen(new ThemedConfirmScreen(confirmed -> {
			if (confirmed) {
				ClientConfig.get().pinnedRecipes.clear();
				ClientConfig.get().save();
			}
			this.minecraft.setScreen(this);
		}, I18n.get(KEY + "delete_all_confirm_title"),
				I18n.get(KEY + "delete_all_confirm_message", count)));
	}

	private void placeRows() {
		for (Row row : this.rows) {
			row.moveTo(LIST_TOP + row.y - this.pane.offset());
		}
	}

	/** "3x Repeater" - the same stack the HUD shows as the result, so a row always says what the HUD would. */
	private static String rowLabel(PinnedRecipe recipe) {
		ItemStack result = PinnedRecipeHud.resultStack(recipe);
		if (result.isEmpty()) {
			return I18n.get(KEY + "unknown_item");
		}
		return result.getCount() + "x " + result.getHoverName().getString();
	}

	private void remove(PinnedRecipe recipe) {
		ClientConfig config = ClientConfig.get();
		config.pinnedRecipes.remove(recipe);
		config.save();
		rebuild();
	}

	private void toggleVisible(Row row) {
		ClientConfig config = ClientConfig.get();
		boolean show = !row.recipe.visible;
		// No more than the HUD has room for - the switch simply stays off.
		if (show && PinnedRecipeManager.visibleCount(config.pinnedRecipes) >= PinnedRecipeManager.MAX_PINNED) {
			return;
		}
		row.recipe.visible = show;
		config.save();
		row.visibleToggle.setMessage(visibleLabel(row.recipe));
	}

	private static String visibleLabel(PinnedRecipe recipe) {
		return I18n.get(recipe.visible ? "options.on" : "options.off");
	}

	private void changeCraftMultiplier(Row row, int delta) {
		int next = Mth.clamp(row.recipe.craftMultiplier + delta, 1, PinnedRecipe.MAX_CRAFT_MULTIPLIER);
		if (next != row.recipe.craftMultiplier) {
			row.recipe.craftMultiplier = next;
			ClientConfig.get().save();
			row.quantity.setValue(String.valueOf(next));
			row.refresh();
		}
	}

	/** What was typed into a row's field: taken over if it is a number of at least 1, capped at the maximum. */
	private void quantityTyped(Row row) {
		String text = row.quantity.getValue();
		// Empty while the player is replacing the whole number - the last valid one stays until then.
		if (text.isEmpty()) {
			return;
		}
		try {
			int typed = Integer.parseInt(text);
			if (typed >= 1) {
				row.recipe.craftMultiplier = Math.min(typed, PinnedRecipe.MAX_CRAFT_MULTIPLIER);
				ClientConfig.get().save();
				row.refresh();
			}
		} catch (NumberFormatException ignored) {
			// The field only takes digits; a number too long for an int is left alone.
		}
	}

	private Row focusedRow() {
		for (Row row : this.rows) {
			if (row.quantity.isFocused()) {
				return row;
			}
		}
		return null;
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		Row row = focusedRow();
		if (row != null && key != GLFW.GLFW_KEY_ESCAPE) {
			row.quantity.keyPressed(key, scanCode, modifiers);
			quantityTyped(row);
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		Row row = focusedRow();
		if (row != null) {
			row.quantity.charTyped(character, modifiers);
			quantityTyped(row);
			return true;
		}
		return super.charTyped(character, modifiers);
	}

	@Override
	public void tick() {
		super.tick();
		// Keeps the text cursors blinking.
		for (Row row : this.rows) {
			row.quantity.tick();
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
		boolean inPane = this.pane.contains(mouseY);
		// Each field takes or drops the keyboard focus depending on whether the click hit it.
		for (Row row : this.rows) {
			row.quantity.setFocus(inPane && row.quantity.isMouseOver(x, y));
		}
		if (button == LEFT_MOUSE_BUTTON && inPane) {
			if (this.pane.mouseClicked(mouseX, mouseY)) {
				return true;
			}
			for (Row row : this.rows) {
				if (row.quantity.isFocused()) {
					row.quantity.mouseClicked(x, y, button);
					return true;
				}
				Button clicked = row.buttonAt(x, y);
				if (clicked == null) {
					continue;
				}
				clicked.playDownSound(this.minecraft.getSoundManager());
				if (clicked == row.minus) {
					changeCraftMultiplier(row, -1);
				} else if (clicked == row.plus) {
					changeCraftMultiplier(row, 1);
				} else if (clicked == row.visibleToggle) {
					toggleVisible(row);
				} else {
					remove(row.recipe);
				}
				return true;
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
		if (this.rows.isEmpty()) {
			MenuText.centered(I18n.get(KEY + "empty"), this.width / 2, LIST_TOP + 10, MESSAGE_COLOR);
		}

		// A button scrolled half out of view must not light up under a cursor that is on the title or on the buttons below.
		int hoverY = this.pane.contains(mouseY) ? mouseY : -1;
		this.pane.beginClip(this.minecraft);
		for (Row row : this.rows) {
			MenuText.text(row.label, row.x, row.minus.y + (ROW_HEIGHT - this.font.lineHeight) / 2, 0xFFFFFF);
			row.minus.render(mouseX, hoverY, partialTick);
			row.quantity.render(mouseX, hoverY, partialTick);
			row.plus.render(mouseX, hoverY, partialTick);
			row.visibleToggle.render(mouseX, hoverY, partialTick);
			row.remove.render(mouseX, hoverY, partialTick);
		}
		this.pane.endClip();
		this.pane.renderScrollbar();
	}

	/** One pinned recipe: its label and, from the right edge inwards, unpin, the switch, and the quantity group. */
	private final class Row {
		final PinnedRecipe recipe;
		final int x;
		/** Top edge, counted from the top of the list. */
		final int y;
		String label;
		final Button minus;
		final EditBox quantity;
		final Button plus;
		final Button visibleToggle;
		final Button remove;

		Row(PinnedRecipe recipe, int x, int y) {
			this.recipe = recipe;
			this.x = x;
			this.y = y;
			int removeX = x + ROW_WIDTH - REMOVE_BUTTON_WIDTH;
			int toggleX = removeX - WIDGET_GAP - VISIBLE_TOGGLE_WIDTH;
			int plusX = toggleX - WIDGET_GAP - QTY_BUTTON_WIDTH;
			int fieldX = plusX - QTY_FIELD_WIDTH;
			int minusX = fieldX - QTY_BUTTON_WIDTH;
			// Clicked by the screen itself, not by the game - so the buttons need no action of their own.
			this.minus = new Button(minusX, 0, QTY_BUTTON_WIDTH, ROW_HEIGHT, "-", pressed -> { });
			// The field's frame lies one pixel outside the box given here.
			this.quantity = new EditBox(PinnedRecipeListScreen.this.font, fieldX + 1, 0, QTY_FIELD_WIDTH - 2, ROW_HEIGHT - 2, I18n.get(KEY + "qty"));
			this.quantity.setMaxLength(String.valueOf(PinnedRecipe.MAX_CRAFT_MULTIPLIER).length());
			this.quantity.setFilter(text -> text.matches("[0-9]*"));
			this.quantity.setValue(String.valueOf(recipe.craftMultiplier));
			this.plus = new Button(plusX, 0, QTY_BUTTON_WIDTH, ROW_HEIGHT, "+", pressed -> { });
			this.visibleToggle = new Button(toggleX, 0, VISIBLE_TOGGLE_WIDTH, ROW_HEIGHT, visibleLabel(recipe), pressed -> { });
			this.remove = new Button(removeX, 0, REMOVE_BUTTON_WIDTH, ROW_HEIGHT, "X", pressed -> { });
			refresh();
		}

		/** After the number of crafts changed: the label names the new total, and the buttons stop at the ends. */
		void refresh() {
			this.label = rowLabel(this.recipe);
			this.minus.active = this.recipe.craftMultiplier > 1;
			this.plus.active = this.recipe.craftMultiplier < PinnedRecipe.MAX_CRAFT_MULTIPLIER;
		}

		void moveTo(int screenY) {
			this.minus.y = screenY;
			this.quantity.y = screenY + 1;
			this.plus.y = screenY;
			this.visibleToggle.y = screenY;
			this.remove.y = screenY;
		}

		Button buttonAt(double mouseX, double mouseY) {
			for (Button button : new Button[] {this.minus, this.plus, this.visibleToggle, this.remove}) {
				if (button.active && button.isMouseOver(mouseX, mouseY)) {
					return button;
				}
			}
			return null;
		}
	}
}
