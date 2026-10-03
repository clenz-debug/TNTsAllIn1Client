package com.tntsallin1client.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Shared logic of the Armor & Tool Status display: durability of the four worn armor pieces and of
 * the held item, or - for stackable items like blocks - the count in that one stack. Used by both
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
				// Not a slot of this version (the Fabric versions have an off hand) - drop it.
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
	static List<Entry> buildEntries(ClientConfig config, PlayerEntity player) {
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
	static Entry buildEntry(ClientConfig config, PlayerEntity player, ArmorStatusSlot slot) {
		ItemStack stack = slot.stack(player);
		if (stack == null) {
			return null;
		}
		String numeric = stack.isDamageable() ? formatDurability(config, stack) : String.valueOf(stack.count);
		String text = config.armorStatusShowName ? stack.getCustomName() + ": " + numeric : numeric;
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
		String text = config.armorStatusShowName ? I18n.translate(slot.labelKey()) + ": " + placeholder : placeholder;
		return new Entry(slot, null, text, config.armorStatusColor);
	}

	/** "current/max", or just "current" with {@link ClientConfig#armorStatusShowMaxDurability} off. */
	private static String formatDurability(ClientConfig config, ItemStack stack) {
		int current = stack.getMaxDamage() - stack.getDamage();
		return config.armorStatusShowMaxDurability ? current + "/" + stack.getMaxDamage() : String.valueOf(current);
	}

	/**
	 * The picked color - also for stackable items in {@link ArmorStatusColorMode#GRADIENT} mode (own
	 * user request in the Fabric versions); only damageable items get the durability gradient.
	 */
	private static int resolveColor(ClientConfig config, ItemStack stack) {
		if (config.armorStatusColorMode == ArmorStatusColorMode.GRADIENT && stack.isDamageable()) {
			float remaining = 1.0f - (float) stack.getDamage() / stack.getMaxDamage();
			// A third of the color wheel: red at 0, yellow in the middle, green at full durability.
			return Color.HSBtoRGB(MathHelper.clamp(remaining, 0.0f, 1.0f) / 3.0f, 1.0f, 1.0f);
		}
		return config.armorStatusColor;
	}

	private static int iconWidth(ClientConfig config) {
		return config.armorStatusShowIcon ? ICON_SIZE + ICON_GAP : 0;
	}

	/** Unscaled height of one row - the icon is taller than a line of text. */
	static int rowHeight(TextRenderer textRenderer, ClientConfig config) {
		return config.armorStatusShowIcon ? Math.max(ICON_SIZE, textRenderer.fontHeight) : textRenderer.fontHeight;
	}

	/** Unscaled width of one row drawn on its own. */
	static int rowWidth(TextRenderer textRenderer, ClientConfig config, Entry entry) {
		return reservedTextWidth(textRenderer, config, entry) + iconWidth(config);
	}

	/**
	 * Text width reserved for one entry: at least as wide as its slot's placeholder, so the icons (and
	 * every entry after it in a horizontal row) stay put instead of shifting whenever the text gets
	 * shorter or longer - e.g. switching the held item from a tool to a stack of blocks. Only wider
	 * than the placeholder when the real text is (long item names).
	 */
	static int reservedTextWidth(TextRenderer textRenderer, ClientConfig config, Entry entry) {
		return Math.max(textRenderer.getStringWidth(entry.text), textRenderer.getStringWidth(buildMaxEntry(config, entry.slot).text));
	}

	private static int maxTextWidth(TextRenderer textRenderer, ClientConfig config, List<Entry> entries) {
		int max = 0;
		for (Entry entry : entries) {
			max = Math.max(max, reservedTextWidth(textRenderer, config, entry));
		}
		return max;
	}

	/** Unscaled width of the entries as one block in the configured direction. */
	static int bundledWidth(TextRenderer textRenderer, ClientConfig config, List<Entry> entries) {
		if (entries.isEmpty()) {
			return 0;
		}
		if (config.armorStatusBundledDirection == ArmorStatusDirection.HORIZONTAL) {
			int width = 0;
			for (Entry entry : entries) {
				width += rowWidth(textRenderer, config, entry);
			}
			return width + (entries.size() - 1) * GAP;
		}
		return maxTextWidth(textRenderer, config, entries) + iconWidth(config);
	}

	/** Unscaled height of the entries as one block in the configured direction. */
	static int bundledHeight(TextRenderer textRenderer, ClientConfig config, List<Entry> entries) {
		if (entries.isEmpty()) {
			return 0;
		}
		int rowHeight = rowHeight(textRenderer, config);
		if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
			return entries.size() * rowHeight + (entries.size() - 1) * GAP;
		}
		return rowHeight;
	}

	/** Draws the entries as one block with its top left corner at `x`/`y`. */
	static void drawBundled(MinecraftClient client, ClientConfig config, List<Entry> entries, int x, int y) {
		TextRenderer textRenderer = client.textRenderer;
		boolean vertical = config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL;
		// In a column every row reserves the widest text, so icons to the right of it line up too.
		int columnWidth = vertical ? maxTextWidth(textRenderer, config, entries) : 0;
		int cursor = 0;
		for (Entry entry : entries) {
			if (vertical) {
				drawRow(client, config, entry, x, y + cursor, columnWidth);
				cursor += rowHeight(textRenderer, config) + GAP;
			} else {
				int reserved = reservedTextWidth(textRenderer, config, entry);
				drawRow(client, config, entry, x + cursor, y, reserved);
				cursor += reserved + iconWidth(config) + GAP;
			}
		}
	}

	/** Draws one row. `textColumnWidth` is the width reserved for the text before an icon on its right. */
	static void drawRow(MinecraftClient client, ClientConfig config, Entry entry, int x, int y, int textColumnWidth) {
		TextRenderer textRenderer = client.textRenderer;
		if (!config.armorStatusShowIcon) {
			textRenderer.drawWithShadow(entry.text, x, y, entry.color);
			return;
		}

		int textY = y + (ICON_SIZE - textRenderer.fontHeight) / 2 + config.armorStatusTextVerticalOffset;
		if (config.armorStatusIconPosition == ArmorStatusIconPosition.RIGHT) {
			textRenderer.drawWithShadow(entry.text, x, textY, entry.color);
			ItemIcons.draw(client, entry.stack, x + textColumnWidth + ICON_GAP, y);
		} else {
			ItemIcons.draw(client, entry.stack, x, y);
			textRenderer.drawWithShadow(entry.text, x + ICON_SIZE + ICON_GAP, textY, entry.color);
		}
	}
}
