package com.tntsallin1client.shulker;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5k rebuild: a small colored grid showing a shulker box's actual
 * contents (all 27 slots, empty ones included so item positions match the
 * real inventory), replacing the original "extra text lines" tooltip
 * extension. Border/background color comes from {@link ShulkerBoxBlock#getColor()}
 * ({@link DyeColor#getTextColor()} - already a vivid, fully-opaque ARGB int,
 * no alpha-byte trap); undyed shulker boxes (null color) fall back to a
 * fixed purple close to their actual default texture.
 *
 * <p>Two-part capture/draw split across {@link com.tntsallin1client.mixin.AbstractContainerScreenMixin}
 * (captures the hovered slot's shulker box stack, if any) and
 * {@link com.tntsallin1client.mixin.ScreenMixin} (draws it) rather than one
 * mixin: the grid must render *after* the vanilla item tooltip so it isn't
 * drawn over, and that only happens once {@code GuiGraphics#renderDeferredElements()}
 * runs, which is called from {@code Screen#renderWithTooltipAndSubtitles} -
 * outside {@code AbstractContainerScreen#render} entirely, and generic
 * across every screen (not container-specific), hence the split.
 *
 * <p><b>Bugfix ("keybind works, display doesn't"):</b> the key gating used
 * to be a plain {@code ModKeyBindings.SHULKER_PREVIEW.isDown()} check. That
 * never becomes true while hovering a slot, because hovering only happens
 * with a container screen open, and vanilla's generic key-state tracking
 * ({@code KeyMapping.set}/{@code .click}, the pair {@code isDown()} and
 * {@code consumeClick()} read from) is itself gated in
 * {@code KeyboardHandler#keyPress} on {@code this.minecraft.screen == null}
 * (or the key being the F3 debug modifier specifically) - the exact same
 * "doesn't fire with a screen open" issue already noted for 5c's inventory
 * sort key, just not previously checked against this feature. Fixed the
 * same way 5c already does it: track press/release via
 * {@link ScreenKeyboardEvents}, which is built for exactly this case,
 * instead of the generic KeyMapping state.
 */
public final class ShulkerPreviewRenderer {
	private static boolean keyHeldInScreen = false;
	private static final int SLOT_SIZE = 18;
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;
	private static final int MARGIN = 4;
	private static final int OFFSET_X = 8;
	private static final int OFFSET_Y = 24;
	private static final int DEFAULT_COLOR = 0xFF8B5FBF;
	private static final int BACKGROUND_COLOR = 0xD0202020;

	private static @Nullable ItemStack pendingStack;
	private static int pendingMouseX;
	private static int pendingMouseY;

	private ShulkerPreviewRenderer() {
	}

	/**
	 * Tracks the preview key's held state per open container screen (chest,
	 * shulker box, player/creative inventory, ...). Resets to not-held on
	 * every newly opened screen rather than relying on a release event always
	 * firing first - {@code ScreenKeyboardEvents} subscriptions are dropped
	 * when a screen closes, so a key still held at that point would otherwise
	 * leave a stale "held" flag for whatever screen opens next.
	 */
	public static void registerScreenTracking() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) {
				return;
			}

			keyHeldInScreen = false;
			ScreenKeyboardEvents.afterKeyPress(screen).register((scr, keyEvent) -> {
				if (ModKeyBindings.SHULKER_PREVIEW.matches(keyEvent)) {
					keyHeldInScreen = true;
				}
			});
			ScreenKeyboardEvents.afterKeyRelease(screen).register((scr, keyEvent) -> {
				if (ModKeyBindings.SHULKER_PREVIEW.matches(keyEvent)) {
					keyHeldInScreen = false;
				}
			});
		});
	}

	/** Called from {@link com.tntsallin1client.mixin.AbstractContainerScreenMixin} once per frame. */
	public static void capture(@Nullable ItemStack hoveredStack, int mouseX, int mouseY) {
		if (hoveredStack != null && isPreviewable(hoveredStack)) {
			pendingStack = hoveredStack;
			pendingMouseX = mouseX;
			pendingMouseY = mouseY;
		} else {
			pendingStack = null;
		}
	}

	/** Called from {@link com.tntsallin1client.mixin.ScreenMixin} after the vanilla tooltip has already drawn. */
	public static void drawIfPending(GuiGraphics guiGraphics) {
		if (pendingStack == null) {
			return;
		}

		ItemContainerContents contents = pendingStack.get(DataComponents.CONTAINER);
		if (contents == null) {
			pendingStack = null;
			return;
		}

		NonNullList<ItemStack> slots = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		contents.copyInto(slots);

		int color = boxColor(pendingStack);
		int gridWidth = COLUMNS * SLOT_SIZE;
		int gridHeight = ROWS * SLOT_SIZE;
		int x = pendingMouseX + OFFSET_X;
		int y = pendingMouseY + OFFSET_Y;

		guiGraphics.fill(x - MARGIN, y - MARGIN, x + gridWidth + MARGIN, y + gridHeight + MARGIN, color);
		guiGraphics.fill(x, y, x + gridWidth, y + gridHeight, BACKGROUND_COLOR);

		var font = Minecraft.getInstance().font;
		for (int index = 0; index < slots.size(); index++) {
			ItemStack slotStack = slots.get(index);
			if (slotStack.isEmpty()) {
				continue;
			}

			int slotX = x + (index % COLUMNS) * SLOT_SIZE + 1;
			int slotY = y + (index / COLUMNS) * SLOT_SIZE + 1;
			guiGraphics.renderItem(slotStack, slotX, slotY);
			guiGraphics.renderItemDecorations(font, slotStack, slotX, slotY);
		}

		pendingStack = null;
	}

	private static boolean isPreviewable(ItemStack stack) {
		return ClientConfig.get().shulkerPreviewEnabled
				&& keyHeldInScreen
				&& stack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof ShulkerBoxBlock
				&& stack.get(DataComponents.CONTAINER) != null;
	}

	private static int boxColor(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock) {
			DyeColor dyeColor = shulkerBoxBlock.getColor();
			if (dyeColor != null) {
				return dyeColor.getTextColor();
			}
		}
		return DEFAULT_COLOR;
	}
}
