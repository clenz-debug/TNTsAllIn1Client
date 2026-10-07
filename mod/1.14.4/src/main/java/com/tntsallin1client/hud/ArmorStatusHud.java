package com.tntsallin1client.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

/**
 * Shared logic of the Armor & Tool Status display: durability of the four worn armor pieces and of
 * the items in both hands, or - for stackable items like blocks - the count in that one stack. Used by both
 * {@link ArmorStatusSlotHud} (one movable HUD element per slot) and {@link ArmorStatusBundledHud}
 * (all slots as a single element), whichever {@link ClientConfig#armorStatusLayoutMode} is active.
 */
public final class ArmorStatusHud {
	private static final int ICON_SIZE = ItemIcons.SIZE;
	private static final int ICON_GAP = 2;
	static final int GAP = 4;

	/** Wider than any real item's durability text gets - only sizes the element's box, never shown. */
	private static final String MAX_DURABILITY_PLACEHOLDER = "9999/9999";
	/** Same placeholder, just the "current" half - for {@link ClientConfig#armorStatusShowMaxDurability} off. */
	private static final String MAX_DURABILITY_ONLY_PLACEHOLDER = "9999";

	private ArmorStatusHud() {
	}

	/** One row: a slot, what is in it (null for a placeholder) and the text and color to show for it. */
	static final class Entry {
		final ArmorStatusSlot slot;
		final ItemStack stack;
		final String text;
		final int color;

		Entry(ArmorStatusSlot slot, ItemStack stack, String text, int color) {
			this.slot = slot;
			this.stack = stack;
			this.text = text;
			this.color = color;
		}
	}

	/** Whether a slot is shown - a slot the player never switched off has no entry and counts as shown. */
	public static boolean isSlotEnabled(ClientConfig config, ArmorStatusSlot slot) {
		Boolean enabled = config.armorStatusSlotEnabled.get(slot.name());
		return enabled == null || enabled;
	}

	/**
	 * The slots in the order the player put them in ({@link ClientConfig#armorStatusSlotOrder}). A
	 * slot missing from that list comes after the listed ones in its natural order, a name in the
	 * list that is no slot is ignored - so a broken saved order never loses a slot.
	 */
	public static List<ArmorStatusSlot> orderedSlots(ClientConfig config) {
		List<ArmorStatusSlot> ordered = new ArrayList<ArmorStatusSlot>(ArmorStatusSlot.values().length);
		for (String name : config.armorStatusSlotOrder) {
			try {
				ArmorStatusSlot slot = ArmorStatusSlot.valueOf(name);
				if (!ordered.contains(slot)) {
					ordered.add(slot);
				}
			} catch (IllegalArgumentException ignored) {
				// Not the name of a slot - drop it.
			}
		}
		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
			if (!ordered.contains(slot)) {
				ordered.add(slot);
			}
		}
		return ordered;
	}

	/** Entries for every shown slot that actually holds something, in {@link #orderedSlots} order. */
	static List<Entry> buildEntries(ClientConfig config, Player player) {
		List<Entry> entries = new ArrayList<Entry>();
		for (ArmorStatusSlot slot : orderedSlots(config)) {
			if (isSlotEnabled(config, slot)) {
				Entry entry = buildEntry(config, player, slot);
				if (entry != null) {
					entries.add(entry);
				}
			}
		}
		return entries;
	}

	/** The entry for one slot, or null if it is empty - there is nothing sensible to show then. */
	static Entry buildEntry(ClientConfig config, Player player, ArmorStatusSlot slot) {
		ItemStack stack = slot.stack(player);
		if (stack.isEmpty()) {
			return null;
		}
		String numeric = stack.isDamageableItem() ? formatDurability(config, stack) : String.valueOf(stack.getCount());
		String text = config.armorStatusShowName ? stack.getHoverName().getString() + ": " + numeric : numeric;
		return new Entry(slot, stack, text, resolveColor(config, stack));
	}

	/**
	 * Placeholder entries, one per shown slot regardless of what is worn or held right now. They give
	 * the element's box its size - for the worst realistic case instead of the current gear, so the
	 * box in the HUD editor doesn't change with every item switched to.
	 */
	static List<Entry> buildMaxEntries(ClientConfig config) {
		List<Entry> entries = new ArrayList<Entry>();
		for (ArmorStatusSlot slot : orderedSlots(config)) {
			if (isSlotEnabled(config, slot)) {
				entries.add(buildMaxEntry(config, slot));
			}
		}
		return entries;
	}

	static Entry buildMaxEntry(ClientConfig config, ArmorStatusSlot slot) {
		String placeholder = config.armorStatusShowMaxDurability ? MAX_DURABILITY_PLACEHOLDER : MAX_DURABILITY_ONLY_PLACEHOLDER;
		String text = config.armorStatusShowName ? I18n.get(slot.labelKey()) + ": " + placeholder : placeholder;
		return new Entry(slot, null, text, config.armorStatusColor);
	}

	/** "current/max", or just "current" with {@link ClientConfig#armorStatusShowMaxDurability} off. */
	private static String formatDurability(ClientConfig config, ItemStack stack) {
		int current = stack.getMaxDamage() - stack.getDamageValue();
		return config.armorStatusShowMaxDurability ? current + "/" + stack.getMaxDamage() : String.valueOf(current);
	}

	/**
	 * The picked color - also for stackable items in {@link ArmorStatusColorMode#GRADIENT} mode (own
	 * user request in the Fabric versions); only damageable items get the durability gradient.
	 */
	private static int resolveColor(ClientConfig config, ItemStack stack) {
		if (config.armorStatusColorMode == ArmorStatusColorMode.GRADIENT && stack.isDamageableItem()) {
			float remaining = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
			// A third of the color wheel: red at 0, yellow in the middle, green at full durability.
			return Color.HSBtoRGB(Mth.clamp(remaining, 0.0f, 1.0f) / 3.0f, 1.0f, 1.0f);
		}
		return config.armorStatusColor;
	}

	private static int iconWidth(ClientConfig config) {
		return config.armorStatusShowIcon ? ICON_SIZE + ICON_GAP : 0;
	}

	/** Unscaled height of one row - the icon is taller than a line of text. */
	static int rowHeight(Font font, ClientConfig config) {
		return config.armorStatusShowIcon ? Math.max(ICON_SIZE, font.lineHeight) : font.lineHeight;
	}

	/** Unscaled width of one row drawn on its own. */
	static int rowWidth(Font font, ClientConfig config, Entry entry) {
		return reservedTextWidth(font, config, entry) + iconWidth(config);
	}

	/**
	 * Text width reserved for one entry: at least as wide as its slot's placeholder, so the icons (and
	 * every entry after it in a horizontal row) stay put instead of shifting whenever the text gets
	 * shorter or longer - e.g. switching the held item from a tool to a stack of blocks. Only wider
	 * than the placeholder when the real text is (long item names).
	 */
	static int reservedTextWidth(Font font, ClientConfig config, Entry entry) {
		return Math.max(font.width(entry.text), font.width(buildMaxEntry(config, entry.slot).text));
	}

	private static int maxTextWidth(Font font, ClientConfig config, List<Entry> entries) {
		int max = 0;
		for (Entry entry : entries) {
			max = Math.max(max, reservedTextWidth(font, config, entry));
		}
		return max;
	}

	/** Unscaled width of the entries as one block in the configured direction. */
	static int bundledWidth(Font font, ClientConfig config, List<Entry> entries) {
		if (entries.isEmpty()) {
			return 0;
		}
		if (config.armorStatusBundledDirection == ArmorStatusDirection.HORIZONTAL) {
			int width = 0;
			for (Entry entry : entries) {
				width += rowWidth(font, config, entry);
			}
			return width + (entries.size() - 1) * GAP;
		}
		return maxTextWidth(font, config, entries) + iconWidth(config);
	}

	/** Unscaled height of the entries as one block in the configured direction. */
	static int bundledHeight(Font font, ClientConfig config, List<Entry> entries) {
		if (entries.isEmpty()) {
			return 0;
		}
		int rowHeight = rowHeight(font, config);
		if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
			return entries.size() * rowHeight + (entries.size() - 1) * GAP;
		}
		return rowHeight;
	}

	/** Draws the entries as one block with its top left corner at `x`/`y`. */
	static void drawBundled(Minecraft client, ClientConfig config, List<Entry> entries, int x, int y) {
		Font font = client.font;
		boolean vertical = config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL;
		// In a column every row reserves the widest text, so icons to the right of it line up too.
		int columnWidth = vertical ? maxTextWidth(font, config, entries) : 0;
		int cursor = 0;
		for (Entry entry : entries) {
			if (vertical) {
				drawRow(client, config, entry, x, y + cursor, columnWidth);
				cursor += rowHeight(font, config) + GAP;
			} else {
				int reserved = reservedTextWidth(font, config, entry);
				drawRow(client, config, entry, x + cursor, y, reserved);
				cursor += reserved + iconWidth(config) + GAP;
			}
		}
	}

	/** Draws one row. `textColumnWidth` is the width reserved for the text before an icon on its right. */
	static void drawRow(Minecraft client, ClientConfig config, Entry entry, int x, int y, int textColumnWidth) {
		Font font = client.font;
		if (!config.armorStatusShowIcon) {
			font.drawShadow(entry.text, x, y, entry.color);
			return;
		}

		int textY = y + (ICON_SIZE - font.lineHeight) / 2 + config.armorStatusTextVerticalOffset;
		if (config.armorStatusIconPosition == ArmorStatusIconPosition.RIGHT) {
			font.drawShadow(entry.text, x, textY, entry.color);
			ItemIcons.draw(client, entry.stack, x + textColumnWidth + ICON_GAP, y);
		} else {
			ItemIcons.draw(client, entry.stack, x, y);
			font.drawShadow(entry.text, x + ICON_SIZE + ICON_GAP, textY, entry.color);
		}
	}
}
