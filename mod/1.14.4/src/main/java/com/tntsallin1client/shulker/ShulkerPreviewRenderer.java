package com.tntsallin1client.shulker;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.lwjgl.glfw.GLFW;

/**
 * Shulker box preview: with the preview key held, hovering a shulker box in any screen with slots
 * shows a small grid of what is in it (all 27 slots, empty ones included so the items sit where
 * they do in the box) instead of the item's tooltip - the same panel as in the newer versions of
 * the mod. The whole panel is tinted in shades of the box's own color: a dark frame, a slightly
 * lighter bar behind the box's name, and the slots.
 *
 * <p>For most colors those shades come from the dye's text color; a few boxes get hand-picked
 * tones sampled from their texture instead, because that color is tuned for sign text, not for
 * the box (black stays pitch black so the slots vanish, yellow is olive, green and lime are neon,
 * light blue is a gray-blue). White gets near-white slots in a light gray frame.
 *
 * <p>The game only keeps track of its key bindings while no screen is open, and the preview is
 * only ever wanted with one open - so whether the key is held is asked from the keyboard (or the
 * mouse, for a binding on a mouse button) directly.
 */
public final class ShulkerPreviewRenderer {
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
	/** In front of the items in the screen's own slots, where the game draws its tooltips too. */
	private static final float ITEM_DEPTH = 300.0F;

	private ShulkerPreviewRenderer() {
	}

	/** Whether the panel takes the place of the stack's tooltip right now. */
	public static boolean isPreviewable(ItemStack stack) {
		return ClientConfig.get().shulkerPreviewEnabled && isShulkerBox(stack) && isKeyHeld();
	}

	/**
	 * Whether the game's own "Diamond x5 / and 3 more..." lines are left out of a shulker box's
	 * tooltip - always while the feature is on, not only while the key is held: the panel replaces them.
	 */
	public static boolean hidesVanillaContents() {
		return ClientConfig.get().shulkerPreviewEnabled;
	}

	private static boolean isKeyHeld() {
		// The binding's key, by the name the game saves it under - there is no other way to ask for it.
		InputConstants.Key key = InputConstants.getKey(ModKeyBindings.SHULKER_PREVIEW.saveString());
		if (key == InputConstants.UNKNOWN) {
			return false;
		}
		long window = Minecraft.getInstance().window.getWindow();
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
		}
		return key.getType() == InputConstants.Type.KEYSYM && InputConstants.isKeyDown(window, key.getValue());
	}

	/** An empty, never filled box counts as well - it has an (empty) inside to show. */
	private static boolean isShulkerBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock;
	}

	/** Draws the panel for the box under the cursor. Called in place of the game drawing the item's tooltip. */
	public static void draw(ItemStack box, int mouseX, int mouseY) {
		NonNullList<ItemStack> slots = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		// A placed box's contents travel with its item in this tag.
		CompoundTag blockEntityTag = box.getTagElement("BlockEntityTag");
		if (blockEntityTag != null && blockEntityTag.contains("Items", 9)) {
			ContainerHelper.loadAllItems(blockEntityTag, slots);
		}

		int[] palette = palette(box);
		int gridWidth = COLUMNS * SLOT_SIZE;
		int gridHeight = ROWS * SLOT_SIZE;
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		int headerHeight = font.lineHeight + HEADER_PADDING;
		int contentX = mouseX + OFFSET_X + BORDER_THICKNESS;
		int contentY = mouseY + OFFSET_Y + BORDER_THICKNESS;
		int x = contentX - BORDER_THICKNESS;
		int y = contentY - BORDER_THICKNESS;

		// Flat, and over everything the screen has drawn so far - the way the game sets up for a tooltip.
		GlStateManager.disableRescaleNormal();
		Lighting.turnOff();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();

		GuiComponent.fill(x, y, x + gridWidth + 2 * BORDER_THICKNESS, y + headerHeight + gridHeight + 2 * BORDER_THICKNESS, palette[BORDER]);
		GuiComponent.fill(contentX, contentY, contentX + gridWidth, contentY + headerHeight, palette[HEADER]);
		font.drawShadow(box.getHoverName().getColoredString(), contentX + 2, contentY + HEADER_PADDING / 2, 0xFFFFFF);

		int gridY = contentY + headerHeight;
		GuiComponent.fill(contentX, gridY, contentX + gridWidth, gridY + gridHeight, palette[GRID_FILL]);
		// The lines between the slots - without them the items on a fill of one color read as a blob.
		for (int col = 0; col <= COLUMNS; col++) {
			int lineX = contentX + col * SLOT_SIZE;
			GuiComponent.fill(lineX, gridY, lineX + 1, gridY + gridHeight, palette[GRID_LINE]);
		}
		for (int row = 0; row <= ROWS; row++) {
			int lineY = gridY + row * SLOT_SIZE;
			GuiComponent.fill(contentX, lineY, contentX + gridWidth, lineY + 1, palette[GRID_LINE]);
		}

		// The items need depth to look right in themselves, and are lifted in front of the screen's own.
		ItemRenderer itemRenderer = client.getItemRenderer();
		itemRenderer.blitOffset = ITEM_DEPTH;
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableDepthTest();
		Lighting.turnOnGui();
		for (int index = 0; index < slots.size(); index++) {
			ItemStack slotStack = slots.get(index);
			if (slotStack.isEmpty()) {
				continue;
			}
			int slotX = contentX + (index % COLUMNS) * SLOT_SIZE + 1;
			int slotY = gridY + (index / COLUMNS) * SLOT_SIZE + 1;
			itemRenderer.renderAndDecorateItem(slotStack, slotX, slotY);
			itemRenderer.renderGuiItemDecorations(font, slotStack, slotX, slotY);
		}
		itemRenderer.blitOffset = 0.0F;

		// What the game leaves behind after a tooltip of its own.
		GlStateManager.enableLighting();
		GlStateManager.enableDepthTest();
		Lighting.turnOn();
		GlStateManager.enableRescaleNormal();
	}

	private static final int BORDER = 0;
	private static final int HEADER = 1;
	private static final int GRID_FILL = 2;
	private static final int GRID_LINE = 3;

	/** Frame, name bar, slots and slot lines, in that order. */
	private static int[] palette(ItemStack stack) {
		DyeColor dyeColor = ((ShulkerBoxBlock) ((BlockItem) stack.getItem()).getBlock()).getColor();
		if (dyeColor == null) {
			// The undyed box: close to its texture's purple.
			return shaded(DEFAULT_COLOR);
		}
		// Hand-picked tones sampled from the shulker textures, see the class comment.
		switch (dyeColor) {
			case WHITE:
				return new int[] {0xFFA5AAAB, 0xFFBEC4C5, 0xFFE6EAEA, 0xFFA5AAAB};
			case BLACK:
				return new int[] {0xFF0A0C10, 0xFF17171B, 0xFF38383C, 0xFF1F1F23};
			case ORANGE:
				return new int[] {0xFF9E4300, 0xFFAE4A00, 0xFFEB6804, 0xFFAE4A00};
			case YELLOW:
				return new int[] {0xFFCC920E, 0xFFD89B0F, 0xFFF8B91A, 0xFFCC920E};
			case LIME:
				return new int[] {0xFF3B6D0E, 0xFF4D8D13, 0xFF61AD19, 0xFF40760F};
			case GREEN:
				return new int[] {0xFF2C3816, 0xFF3B4A1D, 0xFF546D1C, 0xFF3F4E20};
			case LIGHT_BLUE:
				return new int[] {0xFF1B6DA0, 0xFF1D76AF, 0xFF2C9DD3, 0xFF1D76AF};
			default:
				return shaded(dyeColor.getTextColor());
		}
	}

	private static int[] shaded(int color) {
		return new int[] {scale(color, BORDER_SHADE), scale(color, HEADER_SHADE), scale(color, GRID_FILL_SHADE), scale(color, GRID_LINE_SHADE)};
	}

	/** The color darkened to the given share of its brightness, fully opaque. */
	private static int scale(int color, float share) {
		int red = (int) (((color >> 16) & 0xFF) * share);
		int green = (int) (((color >> 8) & 0xFF) * share);
		int blue = (int) ((color & 0xFF) * share);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}
}
