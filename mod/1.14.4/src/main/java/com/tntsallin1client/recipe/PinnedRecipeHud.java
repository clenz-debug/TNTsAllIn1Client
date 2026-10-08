package com.tntsallin1client.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudElement;
import com.tntsallin1client.hud.HudLayout;
import com.tntsallin1client.hud.ItemCounterHud;
import com.tntsallin1client.hud.ItemIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shows every pinned and visible {@link PinnedRecipe} as a row of its own, one below the other: the
 * ingredient icons with their counts, an arrow, then the result. Where the player switched it on,
 * an ingredient that can itself be crafted gets its own ingredients as small icons underneath. Sits
 * at the right edge below the other displays there until moved - the same display as in the newer
 * versions of the mod.
 *
 * <p>A pinned recipe holds what one craft needs; that is first scaled by how often the player wants
 * to craft it ({@link PinnedRecipe#craftMultiplier}). The HUD then counts down against what is in
 * the inventory already: an ingredient the player has enough of drops out of its row, and with all
 * of them there the row is just the arrow and the result - "go craft it". With
 * {@link ClientConfig#pinnedRecipeShowFullAmounts} every ingredient always shows its full amount.
 */
public final class PinnedRecipeHud extends HudElement {
	private static final int EDGE_MARGIN = 4;
	private static final int DEFAULT_TOP = 28;
	private static final int ICON_SIZE = ItemIcons.SIZE;
	private static final int ICON_GAP = 2;
	/** Sub-ingredient icons are half size; their counts are plain text next to them, full size - a count on such an icon would be unreadable. */
	private static final int SUB_ICON_SIZE = 8;
	private static final int SUB_ICON_TEXT_GAP = 1;
	/** After one sub-ingredient's icon and count, before the next. */
	private static final int SUB_ENTRY_GAP = 3;
	private static final int SUB_ROW_GAP = 1;
	/** Between two pinned recipes - wider than between the icons of one, so they read as separate. */
	private static final int RECIPE_ROW_GAP = 4;
	private static final String ARROW = " -> ";

	/** One ingredient ready to draw: the stack (its count already counted down) and its own ingredients. */
	private static final class RowIngredient {
		final ItemStack stack;
		final List<PinnedIngredient> subIngredients;

		RowIngredient(ItemStack stack, List<PinnedIngredient> subIngredients) {
			this.stack = stack;
			this.subIngredients = subIngredients;
		}
	}

	/** One pinned recipe ready to draw. */
	private static final class Row {
		final List<RowIngredient> ingredients;
		final ItemStack result;

		Row(List<RowIngredient> ingredients, ItemStack result) {
			this.ingredients = ingredients;
			this.result = result;
		}
	}

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.pinned_recipe";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.pinnedRecipeEnabled;
	}

	/** Nothing pinned, nothing to move: there is no telling how big the display will be. */
	@Override
	public boolean isEditable(ClientConfig config) {
		return config.pinnedRecipeEnabled && !rows(config, null).isEmpty();
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.pinnedRecipeHudLayout;
	}

	@Override
	protected boolean isHidden(Minecraft client) {
		return client.options.renderDebug;
	}

	@Override
	public int width(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		int width = 0;
		for (Row row : rows(config, client.player)) {
			width = Math.max(width, rowWidth(client.font, row, config.pinnedRecipeShowSubIngredients));
		}
		return width;
	}

	@Override
	public int height(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		int rows = rows(config, client.player).size();
		return rows == 0 ? 0 : rows * rowHeight(client.font, config.pinnedRecipeShowSubIngredients) + (rows - 1) * RECIPE_ROW_GAP;
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return screenWidth - EDGE_MARGIN - width(client);
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		boolean showSub = config.pinnedRecipeShowSubIngredients;
		int rowY = 0;
		for (Row row : rows(config, client.player)) {
			drawRow(client, config, row, rowY, showSub);
			rowY += rowHeight(client.font, showSub) + RECIPE_ROW_GAP;
		}
	}

	private static void drawRow(Minecraft client, ClientConfig config, Row row, int rowY, boolean showSub) {
		Font font = client.font;
		int cursorX = 0;
		for (RowIngredient ingredient : row.ingredients) {
			drawStack(client, ingredient.stack, cursorX, rowY, config.pinnedRecipeCountColor);
			if (showSub && !ingredient.subIngredients.isEmpty()) {
				drawSubIcons(client, ingredient.subIngredients, cursorX, rowY + ICON_SIZE + SUB_ROW_GAP, config.pinnedRecipeSubIngredientCountColor);
			}
			cursorX += columnWidth(font, ingredient, showSub);
		}
		font.drawShadow(ARROW, cursorX, rowY + (ICON_SIZE - font.lineHeight) / 2, config.pinnedRecipeArrowColor);
		cursorX += font.width(ARROW);
		drawStack(client, row.result, cursorX, rowY, config.pinnedRecipeCountColor);
	}

	/**
	 * An item with its count in the lower right corner, where the game puts stack counts - drawn here
	 * rather than by the game, whose own count is always white (the color is the player's choice).
	 * The item is drawn with depth, the text has none: without switching depth off it would end up
	 * behind the item.
	 */
	private static void drawStack(Minecraft client, ItemStack stack, int x, int y, int countColor) {
		ItemIcons.draw(client, stack, x, y);
		if (stack.getCount() != 1) {
			String count = String.valueOf(stack.getCount());
			GlStateManager.disableDepthTest();
			client.font.drawShadow(count, x + 19 - 2 - client.font.width(count), y + 6 + 3, countColor);
			GlStateManager.enableDepthTest();
		}
	}

	/** Each sub-ingredient as a half-size icon followed by its count. */
	private static void drawSubIcons(Minecraft client, List<PinnedIngredient> subIngredients, int x, int y, int countColor) {
		Font font = client.font;
		float iconScale = (float) SUB_ICON_SIZE / ICON_SIZE;
		int textY = y + (SUB_ICON_SIZE - font.lineHeight) / 2;
		int cursorX = 0;
		for (PinnedIngredient sub : subIngredients) {
			ItemStack stack = stackOf(sub.itemId, sub.count);
			if (stack.isEmpty()) {
				continue;
			}
			GlStateManager.pushMatrix();
			GlStateManager.translatef(x + cursorX, y, 0.0F);
			GlStateManager.scalef(iconScale, iconScale, 1.0F);
			ItemIcons.draw(client, stack, 0, 0);
			GlStateManager.popMatrix();
			cursorX += SUB_ICON_SIZE + SUB_ICON_TEXT_GAP;

			String count = String.valueOf(sub.count);
			GlStateManager.disableDepthTest();
			font.drawShadow(count, x + cursorX, textY, countColor);
			GlStateManager.enableDepthTest();
			cursorX += font.width(count) + SUB_ENTRY_GAP;
		}
	}

	/**
	 * The same for every row, whether or not its ingredients have ingredients of their own - so the
	 * rows line up. The strip below is as tall as the taller of its icons and its text.
	 */
	private static int rowHeight(Font font, boolean showSub) {
		return showSub ? ICON_SIZE + SUB_ROW_GAP + Math.max(SUB_ICON_SIZE, font.lineHeight) : ICON_SIZE;
	}

	/** Icons, arrow and result - or further, where an ingredient's strip of sub-ingredients reaches past them. */
	private static int rowWidth(Font font, Row row, boolean showSub) {
		int cursorX = 0;
		int maxExtent = 0;
		for (RowIngredient ingredient : row.ingredients) {
			maxExtent = Math.max(maxExtent, cursorX + ICON_SIZE);
			if (showSub) {
				maxExtent = Math.max(maxExtent, cursorX + subIconsWidth(font, ingredient.subIngredients));
			}
			cursorX += columnWidth(font, ingredient, showSub);
		}
		return Math.max(maxExtent, cursorX + font.width(ARROW) + ICON_SIZE);
	}

	/**
	 * How far one ingredient's column reaches: its icon, or its strip of sub-ingredients where that is
	 * wider. With every column one icon wide, a wide strip ran into the next ingredient's strip and
	 * the two were drawn over each other (own user report).
	 */
	private static int columnWidth(Font font, RowIngredient ingredient, boolean showSub) {
		int width = ICON_SIZE + ICON_GAP;
		if (showSub && !ingredient.subIngredients.isEmpty()) {
			width = Math.max(width, subIconsWidth(font, ingredient.subIngredients));
		}
		return width;
	}

	private static int subIconsWidth(Font font, List<PinnedIngredient> subIngredients) {
		int width = 0;
		for (PinnedIngredient sub : subIngredients) {
			width += SUB_ICON_SIZE + SUB_ICON_TEXT_GAP + font.width(String.valueOf(sub.count)) + SUB_ENTRY_GAP;
		}
		return width;
	}

	/**
	 * The rows to show. `player` null (no world - the HUD editor opened from the title screen): every
	 * ingredient at its full amount, there being no inventory to count down against.
	 */
	private static List<Row> rows(ClientConfig config, Player player) {
		List<Row> rows = new ArrayList<Row>();
		for (PinnedRecipe recipe : config.pinnedRecipes) {
			if (!recipe.visible) {
				continue;
			}
			ItemStack result = resultStack(recipe);
			if (result.isEmpty()) {
				continue;
			}
			List<RowIngredient> ingredients = new ArrayList<RowIngredient>();
			for (PinnedIngredient ingredient : recipe.ingredients) {
				ItemStack full = stackOf(ingredient.itemId, ingredient.count * recipe.craftMultiplier);
				if (full.isEmpty()) {
					continue;
				}
				int shown = full.getCount();
				if (player != null && !config.pinnedRecipeShowFullAmounts) {
					shown -= ItemCounterHud.countInInventory(player, full.getItem());
					if (shown <= 0) {
						continue;
					}
				}
				ingredients.add(new RowIngredient(new ItemStack(full.getItem(), shown), scaledSubIngredients(ingredient.subIngredients, recipe.craftMultiplier)));
			}
			rows.add(new Row(ingredients, result));
		}
		return rows;
	}

	/** Sub-ingredients are kept for one craft of the pinned recipe; how often the player wants to craft it comes in here. */
	private static List<PinnedIngredient> scaledSubIngredients(List<PinnedIngredient> subIngredients, int multiplier) {
		if (subIngredients == null) {
			return Collections.emptyList();
		}
		if (multiplier <= 1 || subIngredients.isEmpty()) {
			return subIngredients;
		}
		List<PinnedIngredient> scaled = new ArrayList<PinnedIngredient>(subIngredients.size());
		for (PinnedIngredient sub : subIngredients) {
			PinnedIngredient copy = new PinnedIngredient();
			copy.itemId = sub.itemId;
			copy.count = sub.count * multiplier;
			scaled.add(copy);
		}
		return scaled;
	}

	/** What the recipe gives in all: its yield per craft times how often the player wants to craft it. Also names its row in the list screen. */
	public static ItemStack resultStack(PinnedRecipe recipe) {
		return stackOf(recipe.resultItemId, recipe.resultCount * recipe.craftMultiplier);
	}

	private static ItemStack stackOf(String itemId, int count) {
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		Item item = id == null ? null : Registry.ITEM.getOptional(id).orElse(null);
		if (item == null || item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, Math.max(1, count));
	}
}
