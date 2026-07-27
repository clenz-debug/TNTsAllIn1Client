package com.tntsallin1client.shulker;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
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
 *
 * <p><b>Follow-up bugfix:</b> that alone still didn't cover a mouse-button
 * binding - {@code KeyMapping#matches(KeyEvent)} only ever matches a
 * KEYSYM-type binding; a mouse button needs the separate
 * {@code matchesMouse(MouseButtonEvent)} method and {@link ScreenMouseEvents}'
 * click/release events instead of the keyboard ones. Since the rebind screen
 * (like vanilla's own Controls screen) allows binding this to either a key
 * or a mouse button, both event families are registered so it works
 * regardless of which one the user picked.
 *
 * <p>Restyled on user request to look like Lunar Client's own shulker preview
 * (reference screenshot supplied): a title bar showing the box's actual
 * display name ("Brown Shulker Box", ...) above the grid, using vanilla's own
 * {@code TooltipRenderUtil#renderTooltipBackground} sprite (the exact
 * background+frame a normal item tooltip draws, since 1.21.x moved that from
 * hardcoded gradient colors to a proper 9-sliced sprite) instead of a plain
 * flat-color rectangle - free native-looking rounded corners/border, no new
 * art needed. The grid itself is now a solid fill derived from the box's own
 * color (previously only a thin border was colored, with a fixed dark gray
 * interior) to match the reference's solid-brown look. Grid lines are kept
 * (confirmed useful in the original 5k feedback round - an all-one-color
 * fill with items on it read as a "blob" without them) and, like the fill
 * itself, derived from the box's own color via {@link ARGB#scaleRGB(int, float)}
 * instead of a fixed gray, so both stay visible against light *and* dark box
 * colors alike instead of only working well against the old fixed dark
 * background.
 *
 * <p><b>Bugfix, immediately after ("middle looks too bright vs. the reference"):</b>
 * the grid fill originally used the box's raw {@code getTextColor()} directly -
 * a vivid, fully-saturated tone. The reference screenshot's grid reads
 * noticeably darker/more muted than that, closer to the surrounding frame's
 * own darkness, not a bright color patch sitting inside a dark border. Now
 * the fill itself is scaled down first ({@code GRID_FILL_SHADE}), with the
 * grid lines scaled down further still from the same raw color so they
 * remain a visibly darker accent on top of the (now also darker) fill.
 *
 * <p><b>Redesign, still same feedback round ("only the middle field changes
 * color - Lunar tints the whole GUI"):</b> the vanilla {@code
 * TooltipRenderUtil} sprite border/header from the previous pass is a fixed
 * neutral color, not tintable - it only ever looked "themed" because the grid
 * next to it happened to be colored. Dropped it in favor of a fully
 * hand-drawn, three-tier panel where *every* layer is a shade of the same
 * {@link #boxColor(ItemStack)}: a dark outer border, a slightly lighter header bar
 * behind the title text, and the (already-existing) mid-tone grid fill -
 * matching the reference screenshot's actual "whole GUI reskinned per
 * shulker color" look instead of "neutral GUI, colored grid".
 */
public final class ShulkerPreviewRenderer {
	private static boolean keyHeldInScreen = false;
	private static final int SLOT_SIZE = 18;
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;
	private static final int OFFSET_X = 8;
	private static final int OFFSET_Y = 24;
	private static final int HEADER_PADDING = 4;
	private static final int BORDER_THICKNESS = 2;
	private static final int DEFAULT_COLOR = 0xFF8B5FBF;
	private static final float BORDER_SHADE = 0.25F;
	private static final float HEADER_SHADE = 0.35F;
	private static final float GRID_FILL_SHADE = 0.55F;
	private static final float GRID_LINE_SHADE = 0.4F;

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
			ScreenMouseEvents.afterMouseClick(screen).register((scr, mouseEvent, consumed) -> {
				if (ModKeyBindings.SHULKER_PREVIEW.matchesMouse(mouseEvent)) {
					keyHeldInScreen = true;
				}
				return false;
			});
			ScreenMouseEvents.afterMouseRelease(screen).register((scr, mouseEvent, consumed) -> {
				if (ModKeyBindings.SHULKER_PREVIEW.matchesMouse(mouseEvent)) {
					keyHeldInScreen = false;
				}
				return false;
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

		NonNullList<ItemStack> slots = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		ItemContainerContents contents = pendingStack.get(DataComponents.CONTAINER);
		if (contents != null) {
			contents.copyInto(slots);
		}

		int color = boxColor(pendingStack);
		int borderColor = ARGB.scaleRGB(color, BORDER_SHADE);
		int headerColor = ARGB.scaleRGB(color, HEADER_SHADE);
		int gridFillColor = ARGB.scaleRGB(color, GRID_FILL_SHADE);
		int gridLineColor = ARGB.scaleRGB(color, GRID_LINE_SHADE);
		int gridWidth = COLUMNS * SLOT_SIZE;
		int gridHeight = ROWS * SLOT_SIZE;
		Font font = Minecraft.getInstance().font;
		Component title = pendingStack.getHoverName();
		int headerHeight = font.lineHeight + HEADER_PADDING;
		int contentX = pendingMouseX + OFFSET_X + BORDER_THICKNESS;
		int contentY = pendingMouseY + OFFSET_Y + BORDER_THICKNESS;
		int x = contentX - BORDER_THICKNESS;
		int y = contentY - BORDER_THICKNESS;

		// Whole panel tinted in shades of the box's own color (dark border, slightly
		// lighter header, mid-tone grid) rather than a neutral vanilla tooltip frame
		// with only the grid colored - matches the reference's "whole GUI reskinned"
		// look instead of "neutral GUI, colored grid".
		guiGraphics.fill(x, y, x + gridWidth + 2 * BORDER_THICKNESS, y + headerHeight + gridHeight + 2 * BORDER_THICKNESS, borderColor);
		guiGraphics.fill(contentX, contentY, contentX + gridWidth, contentY + headerHeight, headerColor);
		guiGraphics.drawString(font, title, contentX + 2, contentY + HEADER_PADDING / 2, 0xFFFFFFFF);

		int gridY = contentY + headerHeight;
		guiGraphics.fill(contentX, gridY, contentX + gridWidth, gridY + gridHeight, gridFillColor);

		// Slot separator lines, a darker shade of the box's own color rather than a fixed gray -
		// the fill above is now the box color itself (not a constant dark background), so a fixed
		// line color would read fine against light boxes but vanish against dark ones.
		for (int col = 0; col <= COLUMNS; col++) {
			int lineX = contentX + col * SLOT_SIZE;
			guiGraphics.fill(lineX, gridY, lineX + 1, gridY + gridHeight, gridLineColor);
		}
		for (int row = 0; row <= ROWS; row++) {
			int lineY = gridY + row * SLOT_SIZE;
			guiGraphics.fill(contentX, lineY, contentX + gridWidth, lineY + 1, gridLineColor);
		}
		for (int index = 0; index < slots.size(); index++) {
			ItemStack slotStack = slots.get(index);
			if (slotStack.isEmpty()) {
				continue;
			}

			int slotX = contentX + (index % COLUMNS) * SLOT_SIZE + 1;
			int slotY = gridY + (index / COLUMNS) * SLOT_SIZE + 1;
			guiGraphics.renderItem(slotStack, slotX, slotY);
			guiGraphics.renderItemDecorations(font, slotStack, slotX, slotY);
		}

		pendingStack = null;
	}

	private static boolean isPreviewable(ItemStack stack) {
		return ClientConfig.get().shulkerPreviewEnabled && keyHeldInScreen && isShulkerBox(stack);
	}

	/** Deliberately doesn't require a present CONTAINER component - an empty/never-filled shulker box is still a valid (empty) box to preview. */
	private static boolean isShulkerBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
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
