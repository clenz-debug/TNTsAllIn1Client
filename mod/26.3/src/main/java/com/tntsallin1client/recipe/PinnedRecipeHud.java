package com.tntsallin1client.recipe;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudLayout;
import com.tntsallin1client.hud.ItemCounterHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5ah, reworked to multi-pin (own user follow-up request, "maximal Hauptrezepte 5 Rezepte"):
 * renders every pinned+visible {@link PinnedRecipe} ({@link ClientConfig#pinnedRecipes}) as its own
 * row, stacked vertically - each row is the ingredient icons (a stack-count badge on each, since
 * several distinct items rules out {@link ItemCounterHud}'s single-icon-plus-adjacent-text approach;
 * see {@link #renderStack} for why that badge is a hand-rolled reproduction of vanilla's own
 * rendering rather than vanilla's rendering itself), a plain arrow, then the result icon+count. When
 * {@link ClientConfig#pinnedRecipeShowSubIngredients} is on, an ingredient that has its own known
 * one-level sub-recipe ({@link PinnedIngredient#subIngredients}) gets a row of small icons drawn
 * underneath it - own user request, "wenn ich z.B. nen Repeater crafte das Rezept der Redstone
 * Fackel [mit] anzeigen". Defaults to hugging the top-right corner below the FPS counter;
 * {@link HudLayout#customPosition} overrides that with a fixed, draggable position/scale (see the
 * HUD editor) for the whole stacked block, exactly like every other HUD element here.
 *
 * <p>{@link PinnedRecipe} itself stores each recipe's full, original per-craft requirement (the
 * one-time snapshot taken at pin time), scaled by {@link PinnedRecipe#craftMultiplier} (own
 * follow-up request - how many times the player wants to craft it) before anything else happens to
 * it. By default the HUD then counts that scaled requirement down live against however much of
 * each item is already in the player's inventory ({@link #buildVisibleRows}), same "sum across main
 * inventory + offhand" logic {@link ItemCounterHud} already uses: an ingredient the player already
 * has enough of simply drops out of its row instead of showing "0x" (not a valid stack count), and
 * once every ingredient in a recipe is satisfied, that row shrinks down to just the arrow and result -
 * a visible "go craft it now" cue rather than the pin silently vanishing. Own follow-up request
 * ("die Items ... nicht verschwinden [lassen] wenn man sie im Inv hat") -
 * {@link ClientConfig#pinnedRecipeShowFullAmounts} turns that countdown off entirely: every
 * ingredient always shows its full scaled requirement, regardless of inventory contents.
 */
public class PinnedRecipeHud implements HudElement {
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_TOP = 28;
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 2;
	/** Sub-ingredient icons are drawn at half size, directly under the ingredient icon they belong
	 * to - vanilla's own stack-count badge isn't used on them (illegible at this size), instead each
	 * gets its own count drawn as plain, full-size text right after it (own user follow-up request:
	 * "die Zahl der benötigten Items soll auch angezeigt werden"). */
	private static final int SUB_ICON_SIZE = 8;
	/** Gap between a sub-icon and its own count text. */
	private static final int SUB_ICON_TEXT_GAP = 1;
	/** Gap after one sub-ingredient's icon+count before the next one starts. */
	private static final int SUB_ENTRY_GAP = 3;
	private static final int SUB_ROW_GAP = 1;
	/** Vertical gap between two different pinned recipes' rows - wider than {@link #ICON_GAP} (which
	 * is between icons *within* one row) so stacked recipes read as visually distinct. */
	private static final int RECIPE_ROW_GAP = 4;
	private static final Component ARROW = Component.literal(" -> ");

	/** One resolved ingredient slot ready to render - the (possibly inventory-countdown-reduced)
	 * stack plus its snapshotted one-level sub-recipe, if any. */
	public record RowIngredient(ItemStack stack, List<PinnedIngredient> subIngredients) {
	}

	/** One pinned recipe's row, ready to render or size - shared by the live HUD and the HUD
	 * editor's drag bounds so neither ever duplicates the other's layout math. */
	public record VisibleRow(List<RowIngredient> ingredients, ItemStack resultStack) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.pinnedRecipeEnabled || config.pinnedRecipes.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		List<VisibleRow> rows = buildVisibleRows(config, player);
		if (rows.isEmpty()) {
			return;
		}

		boolean showSub = config.pinnedRecipeShowSubIngredients;
		HudLayout layout = config.pinnedRecipeHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = defaultX(guiGraphics.guiWidth(), client.font, rows, showSub);
			y = DEFAULT_TOP;
		}

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(layout.scale);
		int cursorY = 0;
		for (VisibleRow row : rows) {
			drawRow(guiGraphics, client.font, row, cursorY, config.pinnedRecipeArrowColor, config.pinnedRecipeCountColor,
					config.pinnedRecipeSubIngredientCountColor, showSub);
			cursorY += rowHeight(client.font, showSub) + RECIPE_ROW_GAP;
		}
		guiGraphics.pose().popMatrix();
	}

	private static void drawRow(GuiGraphicsExtractor guiGraphics, Font font, VisibleRow row, int rowY, int arrowColor, int countColor, int subCountColor, boolean showSub) {
		int cursorX = 0;
		for (RowIngredient ingredient : row.ingredients()) {
			renderStack(guiGraphics, font, ingredient.stack(), cursorX, rowY, countColor);
			if (showSub && !ingredient.subIngredients().isEmpty()) {
				drawSubIcons(guiGraphics, font, ingredient.subIngredients(), cursorX, rowY + ICON_SIZE + SUB_ROW_GAP, subCountColor);
			}
			cursorX += ICON_SIZE + ICON_GAP;
		}
		int arrowWidth = font.width(ARROW);
		guiGraphics.text(font, ARROW, cursorX, rowY + (ICON_SIZE - font.lineHeight) / 2, arrowColor);
		cursorX += arrowWidth;
		renderStack(guiGraphics, font, row.resultStack(), cursorX, rowY, countColor);
	}

	/** Each sub-ingredient as a half-size icon followed by its own plain-text count (full font size,
	 * not shrunk with the icon - a shrunk vanilla count badge would be illegible at {@link #SUB_ICON_SIZE}).
	 * Own follow-up request ("die Zahlen sollen wie die Pfeile auch von der Farbe her angepasst
	 * werden") - {@code countColor} is {@link ClientConfig#pinnedRecipeSubIngredientCountColor};
	 * unlike the main ingredient/result counts (vanilla's own badge via
	 * {@link GuiGraphicsExtractor#itemDecorations}, no color hook available), this text is our own
	 * plain draw, so it actually can be recolored. */
	private static void drawSubIcons(GuiGraphicsExtractor guiGraphics, Font font, List<PinnedIngredient> subIngredients, int x, int y, int countColor) {
		float iconScale = (float) SUB_ICON_SIZE / ICON_SIZE;
		int textY = y + (SUB_ICON_SIZE - font.lineHeight) / 2;
		int cursorX = 0;
		for (PinnedIngredient sub : subIngredients) {
			ItemStack stack = stackOf(sub.itemId, sub.count);
			if (stack.isEmpty()) {
				continue;
			}
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate(x + cursorX, y);
			guiGraphics.pose().scale(iconScale);
			guiGraphics.item(stack, 0, 0);
			guiGraphics.pose().popMatrix();
			cursorX += SUB_ICON_SIZE + SUB_ICON_TEXT_GAP;

			String countText = String.valueOf(sub.count);
			guiGraphics.text(font, countText, x + cursorX, textY, countColor);
			cursorX += font.width(countText) + SUB_ENTRY_GAP;
		}
	}

	/**
	 * Own follow-up request ("die Farben bei den Zahlen der Hauptrezepte sollen ... einstellbar
	 * sein") - {@link GuiGraphicsExtractor#itemDecorations} hardcodes its count text to white with no
	 * color parameter, so this reproduces that method's own three private helpers by hand instead
	 * (confirmed against Minecraft's decompiled source: {@code itemBar}/{@code itemCooldown}/
	 * {@code itemCount}, same order, same pixel math), with only the count text's color pulled
	 * out as {@code countColor} (= {@link ClientConfig#pinnedRecipeCountColor}) instead of vanilla's
	 * hardcoded {@code -1}. The durability bar and cooldown overlay are reproduced unchanged, purely
	 * for parity with the vanilla call this replaces - the display stacks here are always freshly
	 * constructed with no damage, so the bar in practice never actually shows, but keeping it means
	 * this stays a genuine drop-in replacement rather than a narrower reimplementation that could
	 * silently diverge from vanilla later (e.g. if a future change ever gave these stacks real NBT).
	 */
	private static void renderStack(GuiGraphicsExtractor guiGraphics, Font font, ItemStack stack, int x, int y, int countColor) {
		guiGraphics.item(stack, x, y);
		if (stack.isEmpty()) {
			return;
		}
		if (stack.isBarVisible()) {
			int barX = x + 2;
			int barY = y + 13;
			guiGraphics.fill(barX, barY, barX + 13, barY + 2, 0xFF000000);
			guiGraphics.fill(barX, barY, barX + stack.getBarWidth(), barY + 1, ARGB.opaque(stack.getBarColor()));
		}
		LocalPlayer player = Minecraft.getInstance().player;
		float cooldown = player == null ? 0.0F
				: player.getCooldowns().getCooldownPercent(stack, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
		if (cooldown > 0.0F) {
			int cooldownTop = y + Mth.floor(16.0F * (1.0F - cooldown));
			int cooldownBottom = cooldownTop + Mth.ceil(16.0F * cooldown);
			guiGraphics.fill(x, cooldownTop, x + 16, cooldownBottom, Integer.MAX_VALUE);
		}
		if (stack.getCount() != 1) {
			String countText = String.valueOf(stack.getCount());
			guiGraphics.text(font, countText, x + 19 - 2 - font.width(countText), y + 6 + 3, countColor, true);
		}
	}

	/** Right-aligned default X for the un-customized position - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX(int guiWidth, Font font, List<VisibleRow> rows, boolean showSub) {
		return guiWidth - DEFAULT_RIGHT_MARGIN - contentWidth(font, rows, showSub);
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}

	/** Unscaled height of one row, with or without the sub-ingredient strip reserved - fixed per row
	 * regardless of whether that specific row's ingredients actually have sub-recipes, so every row
	 * lines up instead of jittering row-to-row (own design choice, simpler/more robust than
	 * per-row-variable heights). The sub-strip's own height is whichever is taller, the icon or the
	 * count text next to it (same {@code Math.max(ICON_SIZE, font.lineHeight)} reasoning
	 * {@code ArmorStatusHud#contentHeight} already uses for its own icon+text rows). */
	public static int rowHeight(Font font, boolean showSub) {
		return showSub ? ICON_SIZE + SUB_ROW_GAP + Math.max(SUB_ICON_SIZE, font.lineHeight) : ICON_SIZE;
	}

	/** Unscaled pixel width of one row - its ingredient icons + arrow + result icon, widened to fit
	 * any ingredient's own sub-icons-and-counts strip if that reaches further right than the row's
	 * own icons would (only relevant when {@code showSub} is on; a sub-strip commonly does run wider
	 * than its single 16px parent column once counts are drawn next to each sub-icon). */
	public static int rowWidth(Font font, VisibleRow row, boolean showSub) {
		int cursorX = 0;
		int maxExtent = 0;
		for (RowIngredient ingredient : row.ingredients()) {
			maxExtent = Math.max(maxExtent, cursorX + ICON_SIZE);
			if (showSub && !ingredient.subIngredients().isEmpty()) {
				maxExtent = Math.max(maxExtent, cursorX + subIconsWidth(font, ingredient.subIngredients()));
			}
			cursorX += ICON_SIZE + ICON_GAP;
		}
		int tailWidth = font.width(ARROW) + ICON_SIZE;
		return Math.max(maxExtent, cursorX + tailWidth);
	}

	/** Unscaled pixel width of one ingredient's full sub-icons-and-counts strip. */
	private static int subIconsWidth(Font font, List<PinnedIngredient> subIngredients) {
		int width = 0;
		for (PinnedIngredient sub : subIngredients) {
			width += SUB_ICON_SIZE + SUB_ICON_TEXT_GAP + font.width(String.valueOf(sub.count)) + SUB_ENTRY_GAP;
		}
		return width;
	}

	/** Widest row's width - shared with the HUD editor's drag bounds. */
	public static int contentWidth(Font font, List<VisibleRow> rows, boolean showSub) {
		int max = 0;
		for (VisibleRow row : rows) {
			max = Math.max(max, rowWidth(font, row, showSub));
		}
		return max;
	}

	/** Every row stacked, with the gap between them - shared with the HUD editor's drag bounds. */
	public static int contentHeight(Font font, List<VisibleRow> rows, boolean showSub) {
		if (rows.isEmpty()) {
			return 0;
		}
		return rows.size() * rowHeight(font, showSub) + Math.max(0, rows.size() - 1) * RECIPE_ROW_GAP;
	}

	/** Live render rows: every pinned, currently-visible recipe. Each ingredient's requirement is
	 * scaled by {@link PinnedRecipe#craftMultiplier} first (own follow-up request: "die Menge an
	 * Items die man vom gepinnten Rezept craften will"), then, unless
	 * {@link ClientConfig#pinnedRecipeShowFullAmounts} is on, counted down against the player's
	 * actual inventory and dropped once satisfied - see this class's own doc comment. With that
	 * setting on (own follow-up request: "die Items ... nicht verschwinden [lassen] wenn man sie im
	 * Inv hat"), every ingredient always shows its full scaled requirement instead, same as
	 * {@link #buildMaxVisibleRows}. */
	public static List<VisibleRow> buildVisibleRows(ClientConfig config, LocalPlayer player) {
		List<VisibleRow> rows = new ArrayList<>();
		for (PinnedRecipe recipe : config.pinnedRecipes) {
			if (!recipe.visible) {
				continue;
			}
			ItemStack resultStack = resultStack(recipe);
			if (resultStack.isEmpty()) {
				continue;
			}
			List<RowIngredient> ingredients = new ArrayList<>();
			for (PinnedIngredient ingredient : recipe.ingredients) {
				ItemStack fullStack = stackOf(ingredient.itemId, ingredient.count * recipe.craftMultiplier);
				if (fullStack.isEmpty()) {
					continue;
				}
				int shown = fullStack.getCount();
				if (!config.pinnedRecipeShowFullAmounts) {
					int have = ItemCounterHud.countInInventory(player, fullStack.getItem());
					shown -= have;
					if (shown <= 0) {
						continue;
					}
				}
				ingredients.add(new RowIngredient(new ItemStack(fullStack.getItem(), shown),
						scaledSubIngredients(ingredient.subIngredients, recipe.craftMultiplier)));
			}
			rows.add(new VisibleRow(ingredients, resultStack));
		}
		return rows;
	}

	/**
	 * Editor-only: every pinned+visible recipe sized at its FULL, un-countdown requirement (worst
	 * realistic case, same "size for the max, not the live-reduced state" reasoning
	 * {@code ArmorStatusHud#buildMaxEntries} already uses) - for when there's no live player to
	 * count down against yet (e.g. title screen) but a recipe is already pinned from a previous
	 * session.
	 */
	public static List<VisibleRow> buildMaxVisibleRows(ClientConfig config) {
		List<VisibleRow> rows = new ArrayList<>();
		for (PinnedRecipe recipe : config.pinnedRecipes) {
			if (!recipe.visible) {
				continue;
			}
			ItemStack resultStack = resultStack(recipe);
			if (resultStack.isEmpty()) {
				continue;
			}
			List<RowIngredient> ingredients = new ArrayList<>();
			for (PinnedIngredient ingredient : recipe.ingredients) {
				ItemStack stack = stackOf(ingredient.itemId, ingredient.count * recipe.craftMultiplier);
				if (!stack.isEmpty()) {
					ingredients.add(new RowIngredient(stack, scaledSubIngredients(ingredient.subIngredients, recipe.craftMultiplier)));
				}
			}
			rows.add(new VisibleRow(ingredients, resultStack));
		}
		return rows;
	}

	/** {@link PinnedIngredient#subIngredients} are stored scaled for one craft of the parent
	 * recipe/pin (see {@code PinnedRecipeManager#findSubIngredients}) - {@link PinnedRecipe#craftMultiplier}
	 * is only ever applied at render time, so this builds transient, display-only copies rather than
	 * mutating the stored snapshot. */
	private static List<PinnedIngredient> scaledSubIngredients(List<PinnedIngredient> subIngredients, int multiplier) {
		if (multiplier <= 1 || subIngredients.isEmpty()) {
			return subIngredients;
		}
		List<PinnedIngredient> scaled = new ArrayList<>(subIngredients.size());
		for (PinnedIngredient sub : subIngredients) {
			PinnedIngredient copy = new PinnedIngredient();
			copy.itemId = sub.itemId;
			copy.count = sub.count * multiplier;
			scaled.add(copy);
		}
		return scaled;
	}

	/** Editor-only, only used when nothing is pinned yet at all - one representative example row so
	 * the drag box can still be positioned ahead of time, same reasoning as every other HUD editor
	 * entry's own placeholder. {@link ItemStack#EMPTY} is fine here (same convention
	 * {@code ArmorStatusHud#buildMaxEntry} uses): only the ingredient *count* feeds the sizing math,
	 * the icons themselves are never actually drawn from this list. */
	public static List<VisibleRow> buildPlaceholderVisibleRows(int ingredientCount) {
		List<RowIngredient> ingredients = new ArrayList<>();
		for (int i = 0; i < ingredientCount; i++) {
			ingredients.add(new RowIngredient(ItemStack.EMPTY, List.of()));
		}
		return List.of(new VisibleRow(ingredients, ItemStack.EMPTY));
	}

	/** Scaled by {@link PinnedRecipe#craftMultiplier} - the recipe's own per-craft yield
	 * ({@link PinnedRecipe#resultCount}) times how many crafts the player wants, so both the HUD row
	 * and {@code PinnedRecipeListScreen}'s row label (which sources its label from this same method)
	 * show the actual total the player is after. */
	public static ItemStack resultStack(PinnedRecipe recipe) {
		return stackOf(recipe.resultItemId, recipe.resultCount * recipe.craftMultiplier);
	}

	private static ItemStack stackOf(String itemId, int count) {
		Identifier id = Identifier.tryParse(itemId);
		if (id == null) {
			return ItemStack.EMPTY;
		}
		@Nullable Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, Math.max(1, count));
	}
}
