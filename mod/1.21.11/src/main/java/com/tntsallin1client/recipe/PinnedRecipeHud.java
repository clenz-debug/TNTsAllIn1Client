package com.tntsallin1client.recipe;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudLayout;
import com.tntsallin1client.hud.ItemCounterHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5ah, extended same phase (Nutzer-Nachtrag - "Anzahl der benötigten Materialien
 * runterzählen, wenn man ein Item was benötigt wird ins Inventar packt"): renders the recipe
 * {@link PinnedRecipeManager} last pinned - a row of ingredient icons (each with vanilla's own
 * stack-count badge, since several distinct items rules out {@link ItemCounterHud}'s single-icon-
 * plus-adjacent-text approach), a plain arrow, then the result icon+count. Defaults to hugging the
 * top-right corner below the FPS counter; {@link HudLayout#customPosition} overrides that with a
 * fixed, draggable position/scale (see the HUD editor), exactly like every other HUD element here.
 *
 * <p>{@link PinnedRecipe} itself stores the recipe's full, original requirement (the one-time
 * snapshot taken at pin time) - the HUD counts that down live against however much of each item
 * is already in the player's inventory ({@link #remainingIngredientStacks}), same "sum across
 * main inventory + offhand" logic {@link ItemCounterHud} already uses. An ingredient the player
 * already has enough of simply drops out of the row instead of showing "0x" (not a valid stack
 * count); once every ingredient is satisfied, the row shrinks down to just the arrow and result -
 * a visible "go craft it now" cue rather than the pin silently vanishing.
 */
public class PinnedRecipeHud implements HudElement {
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_TOP = 28;
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 2;
	private static final Component ARROW = Component.literal(" -> ");

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.pinnedRecipeEnabled || config.pinnedRecipe == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		List<ItemStack> ingredientStacks = remainingIngredientStacks(config.pinnedRecipe, player);
		ItemStack resultStack = resultStack(config.pinnedRecipe);
		if (resultStack.isEmpty()) {
			return;
		}

		HudLayout layout = config.pinnedRecipeHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = defaultX(guiGraphics.guiWidth(), client.font, ingredientStacks.size());
			y = DEFAULT_TOP;
		}

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(layout.scale);
		int cursorX = 0;
		for (ItemStack stack : ingredientStacks) {
			renderStack(guiGraphics, client.font, stack, cursorX, 0);
			cursorX += ICON_SIZE + ICON_GAP;
		}
		int arrowWidth = client.font.width(ARROW);
		guiGraphics.drawString(client.font, ARROW, cursorX, (ICON_SIZE - client.font.lineHeight) / 2, 0xFFFFFFFF);
		cursorX += arrowWidth;
		renderStack(guiGraphics, client.font, resultStack, cursorX, 0);
		guiGraphics.pose().popMatrix();
	}

	private static void renderStack(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y) {
		guiGraphics.renderItem(stack, x, y);
		guiGraphics.renderItemDecorations(font, stack, x, y);
	}

	/** Right-aligned default X for the un-customized position - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX(int guiWidth, Font font, int ingredientCount) {
		return guiWidth - DEFAULT_RIGHT_MARGIN - contentWidth(font, ingredientCount);
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}

	/** Unscaled pixel width of the whole row (N ingredient icons + arrow + result icon) - shared with the HUD editor's drag bounds. */
	public static int contentWidth(Font font, int ingredientCount) {
		int ingredientsWidth = ingredientCount * ICON_SIZE + Math.max(0, ingredientCount - 1) * ICON_GAP;
		return ingredientsWidth + font.width(ARROW) + ICON_SIZE;
	}

	public static int contentHeight() {
		return ICON_SIZE;
	}

	/** Resolves a {@link PinnedRecipe}'s stored item ids back into real {@link ItemStack}s at the
	 * recipe's full, original requirement - no inventory countdown applied. Unknown/removed item
	 * ids (e.g. from a datapack no longer active) are simply dropped rather than shown as "air". */
	public static List<ItemStack> ingredientStacks(PinnedRecipe recipe) {
		List<ItemStack> stacks = new ArrayList<>();
		for (PinnedIngredient ingredient : recipe.ingredients) {
			ItemStack stack = stackOf(ingredient.itemId, ingredient.count);
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	/** Same as {@link #ingredientStacks}, but each count is reduced by however many the player
	 * already has ({@link ItemCounterHud#countInInventory}) - an ingredient the player already has
	 * enough of is left out entirely (a stack of count 0 isn't meaningful to render). Shared by
	 * both the actual render and the HUD editor's drag-box sizing, so the drag handles always match
	 * whatever is actually drawn underneath. */
	public static List<ItemStack> remainingIngredientStacks(PinnedRecipe recipe, LocalPlayer player) {
		List<ItemStack> stacks = new ArrayList<>();
		for (ItemStack fullStack : ingredientStacks(recipe)) {
			int have = ItemCounterHud.countInInventory(player, fullStack.getItem());
			int remaining = fullStack.getCount() - have;
			if (remaining > 0) {
				stacks.add(new ItemStack(fullStack.getItem(), remaining));
			}
		}
		return stacks;
	}

	public static ItemStack resultStack(PinnedRecipe recipe) {
		return stackOf(recipe.resultItemId, recipe.resultCount);
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
