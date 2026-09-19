package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared logic for the Armor & Tool Status display: durability of the four
 * worn armor pieces plus main-hand/offhand, or - for stackable items like
 * blocks - the count in that one stack, never the whole inventory (unlike
 * {@link ItemCounterHud}). Used by both {@link ArmorStatusSlotHud} (one
 * draggable HUD element per slot) and {@link ArmorStatusBundledHud} (all
 * enabled slots as a single element), whichever
 * {@link ClientConfig#armorStatusLayoutMode} is active, plus by the HUD
 * editor for accurate drag bounds - same "no duplicated layout math"
 * convention as every other HUD element in this project.
 */
public final class ArmorStatusHud {
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 2;
	public static final int GAP = 4;

	/** Wider than any real item's durability text ever gets (highest is netherite tools at 2031/2031) - used only for {@link #buildMaxEntry}'s editor-sizing placeholder, never shown live. */
	private static final String MAX_DURABILITY_PLACEHOLDER = "9999/9999";
	/** Same placeholder, just the "current" half - for {@link ClientConfig#armorStatusShowMaxDurability} off. */
	private static final String MAX_DURABILITY_ONLY_PLACEHOLDER = "9999";

	private ArmorStatusHud() {
	}

	public record Entry(ArmorStatusSlot slot, ItemStack stack, String text, int color) {
	}

	/** Whether a slot's display is turned on - absent from the map (nothing toggled off yet) counts as enabled, same convention as {@link ClientConfig#keystrokesKeyEnabled}. */
	public static boolean isSlotEnabled(ClientConfig config, ArmorStatusSlot slot) {
		return config.armorStatusSlotEnabled.getOrDefault(slot.name(), true);
	}

	/**
	 * {@link ArmorStatusSlot#values()}, reordered per {@link ClientConfig#armorStatusSlotOrder} -
	 * any slot missing from that list (nothing customized yet, or a slot an update added since)
	 * is appended afterward in {@link ArmorStatusSlot}'s own declaration order, and any stale
	 * name in the list that's no longer a real slot is ignored, so a corrupt/outdated saved
	 * order degrades to the natural order instead of dropping a slot entirely.
	 */
	public static List<ArmorStatusSlot> orderedSlots(ClientConfig config) {
		List<ArmorStatusSlot> ordered = new ArrayList<>(ArmorStatusSlot.values().length);
		for (String name : config.armorStatusSlotOrder) {
			try {
				ordered.add(ArmorStatusSlot.valueOf(name));
			} catch (IllegalArgumentException ignored) {
				// Stale entry from a slot that no longer exists - drop it.
			}
		}
		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
			if (!ordered.contains(slot)) {
				ordered.add(slot);
			}
		}
		return ordered;
	}

	/** Entries for every enabled slot that's actually holding/wearing something, in {@link #orderedSlots} order. */
	public static List<Entry> buildEntries(ClientConfig config, LocalPlayer player) {
		List<ArmorStatusSlot> slots = orderedSlots(config);
		List<Entry> entries = new ArrayList<>(slots.size());
		for (ArmorStatusSlot slot : slots) {
			if (!isSlotEnabled(config, slot)) {
				continue;
			}
			Entry entry = buildEntry(config, player, slot);
			if (entry != null) {
				entries.add(entry);
			}
		}
		return entries;
	}

	/** The entry for one slot, or null if that slot is currently empty - there's nothing sensible to show then. */
	public static @Nullable Entry buildEntry(ClientConfig config, LocalPlayer player, ArmorStatusSlot slot) {
		ItemStack stack = player.getItemBySlot(slot.vanillaSlot);
		if (stack.isEmpty()) {
			return null;
		}

		String numeric = stack.isDamageableItem()
				? formatDurability(config, stack)
				: String.valueOf(stack.getCount());
		String text = config.armorStatusShowName ? stack.getHoverName().getString() + ": " + numeric : numeric;
		return new Entry(slot, stack, text, resolveColor(config, stack));
	}

	/**
	 * Editor-only synthetic entries, one per enabled slot regardless of what's actually
	 * worn/held right now - sized for the worst realistic case ("armor worn, offhand used" per
	 * user request) instead of the player's current gear, so the HUD editor's drag/resize box
	 * never has to be redone later just because different, longer-durability gear got equipped.
	 * The live HUD itself keeps using {@link #buildEntries} untouched; this is for
	 * {@code HudEditorScreen}'s bounds calculation only.
	 */
	public static List<Entry> buildMaxEntries(ClientConfig config) {
		List<ArmorStatusSlot> slots = orderedSlots(config);
		List<Entry> entries = new ArrayList<>(slots.size());
		for (ArmorStatusSlot slot : slots) {
			if (isSlotEnabled(config, slot)) {
				entries.add(buildMaxEntry(config, slot));
			}
		}
		return entries;
	}

	/** The one-slot version of {@link #buildMaxEntries}, for the per-slot layout mode's own editor bounds. */
	public static Entry buildMaxEntry(ClientConfig config, ArmorStatusSlot slot) {
		String label = Component.translatable("gui.tntsallin1client.armor_status_slot." + slot.name().toLowerCase(Locale.ROOT)).getString();
		String placeholder = config.armorStatusShowMaxDurability ? MAX_DURABILITY_PLACEHOLDER : MAX_DURABILITY_ONLY_PLACEHOLDER;
		String text = config.armorStatusShowName ? label + ": " + placeholder : placeholder;
		return new Entry(slot, ItemStack.EMPTY, text, config.armorStatusColor);
	}

	/** "current/max", or just "current" with {@link ClientConfig#armorStatusShowMaxDurability} off. */
	private static String formatDurability(ClientConfig config, ItemStack stack) {
		int current = stack.getMaxDamage() - stack.getDamageValue();
		return config.armorStatusShowMaxDurability ? current + "/" + stack.getMaxDamage() : String.valueOf(current);
	}

	/**
	 * The fixed standard color in {@link ArmorStatusColorMode#FIXED} mode - and always for stackable
	 * items even in {@link ArmorStatusColorMode#GRADIENT} mode, per user request ("stackbare Items
	 * sollen immer ohne diesen Verlauf angezeigt werden, mit gewählter Farbe"). Only damageable items
	 * get the remaining-durability gradient.
	 */
	private static int resolveColor(ClientConfig config, ItemStack stack) {
		if (config.armorStatusColorMode == ArmorStatusColorMode.GRADIENT && stack.isDamageableItem()) {
			float remaining = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
			return durabilityGradientColor(remaining);
		}
		return config.armorStatusColor;
	}

	/** Green at full durability fading through yellow to red near breaking - same hue range vanilla's own durability bar sweeps, just smooth instead of stepped. */
	private static int durabilityGradientColor(float remainingFraction) {
		float hue = Mth.clamp(remainingFraction, 0.0f, 1.0f) / 3.0f;
		return Mth.hsvToArgb(hue, 1.0f, 1.0f, 255);
	}

	/** Unscaled pixel width of one row (icon + gap + text, or just text) - shared with the HUD editor's drag bounds. */
	public static int contentWidth(Font font, String text, boolean showIcon) {
		int textWidth = font.width(text);
		return showIcon ? textWidth + ICON_SIZE + ICON_GAP : textWidth;
	}

	/** Unscaled pixel height of one row - the 16px icon is taller than a text line once it's shown. */
	public static int contentHeight(Font font, boolean showIcon) {
		return showIcon ? Math.max(ICON_SIZE, font.lineHeight) : font.lineHeight;
	}

	/** Draws one row at an already-translated/scaled local origin - shared by both the individual and bundled HUD elements. */
	public static void drawRow(GuiGraphicsExtractor guiGraphics, Font font, Entry entry, int localX, int localY, boolean showIcon, ArmorStatusIconPosition iconPosition) {
		if (!showIcon) {
			guiGraphics.text(font, entry.text(), localX, localY, entry.color());
			return;
		}

		int textY = localY + (ICON_SIZE - font.lineHeight) / 2;
		if (iconPosition == ArmorStatusIconPosition.RIGHT) {
			guiGraphics.text(font, entry.text(), localX, textY, entry.color());
			guiGraphics.item(entry.stack(), localX + font.width(entry.text()) + ICON_GAP, localY);
		} else {
			guiGraphics.item(entry.stack(), localX, localY);
			guiGraphics.text(font, entry.text(), localX + ICON_SIZE + ICON_GAP, textY, entry.color());
		}
	}

	/** Total bundled width for the current direction - shared with the HUD editor's drag bounds. */
	public static int bundledWidth(Font font, ClientConfig config, List<Entry> entries) {
		boolean showIcon = config.armorStatusShowIcon;
		if (config.armorStatusBundledDirection == ArmorStatusDirection.HORIZONTAL) {
			int width = 0;
			for (Entry entry : entries) {
				width += contentWidth(font, entry.text(), showIcon);
			}
			return width + Math.max(0, entries.size() - 1) * GAP;
		}
		int max = 0;
		for (Entry entry : entries) {
			max = Math.max(max, contentWidth(font, entry.text(), showIcon));
		}
		return max;
	}

	/** Total bundled height for the current direction - shared with the HUD editor's drag bounds. */
	public static int bundledHeight(Font font, ClientConfig config, List<Entry> entries) {
		int rowHeight = contentHeight(font, config.armorStatusShowIcon);
		if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
			return entries.size() * rowHeight + Math.max(0, entries.size() - 1) * GAP;
		}
		return rowHeight;
	}

	/**
	 * Adjusts {@code armorStatusBundledHudLayout}'s x/y so the block's on-screen position doesn't
	 * jump the moment {@link ClientConfig#armorStatusBundledReversed} flips - its meaning
	 * (top-left vs. the far edge the block grows away from) changes with the setting, so without
	 * this the same stored number would suddenly point somewhere else on screen. Called from the
	 * options screen right before the toggle is actually applied to config. Uses the same
	 * max-durability placeholder sizing as the HUD editor's own drag box (see {@link #buildMaxEntries})
	 * so the correction doesn't depend on what's currently equipped/held.
	 */
	public static void adjustBundledPositionForReversedToggle(ClientConfig config, Font font, boolean newReversed) {
		List<Entry> entries = buildMaxEntries(config);
		if (entries.isEmpty()) {
			return;
		}

		HudLayout layout = config.armorStatusBundledHudLayout;
		if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
			float delta = bundledHeight(font, config, entries) * layout.scale;
			layout.y += newReversed ? delta : -delta;
		} else {
			float delta = bundledWidth(font, config, entries) * layout.scale;
			layout.x += newReversed ? delta : -delta;
		}
	}

	/**
	 * Draws every entry as one bundled block, translated/scaled onto the given origin by the
	 * caller's own pose-stack push. With {@link ClientConfig#armorStatusBundledReversed}, (x, y)
	 * is the edge the block grows away from rather than its top-left - the origin is shifted back
	 * by the block's own current size so that edge stays put as entries are added/removed instead
	 * of the far edge. {@link com.tntsallin1client.menu.HudEditorScreen#armorStatusBundledBounds}
	 * mirrors this exact offset so the editor's drag box lines up with what's actually drawn here.
	 */
	public static void drawBundled(GuiGraphicsExtractor guiGraphics, Font font, ClientConfig config, List<Entry> entries, float x, float y, float scale) {
		boolean showIcon = config.armorStatusShowIcon;
		int rowHeight = contentHeight(font, showIcon);

		float originX = x;
		float originY = y;
		if (config.armorStatusBundledReversed) {
			if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
				originY -= bundledHeight(font, config, entries) * scale;
			} else {
				originX -= bundledWidth(font, config, entries) * scale;
			}
		}

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(originX, originY);
		guiGraphics.pose().scale(scale);

		int cursor = 0;
		for (Entry entry : entries) {
			if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
				drawRow(guiGraphics, font, entry, 0, cursor, showIcon, config.armorStatusIconPosition);
				cursor += rowHeight + GAP;
			} else {
				drawRow(guiGraphics, font, entry, cursor, 0, showIcon, config.armorStatusIconPosition);
				cursor += contentWidth(font, entry.text(), showIcon) + GAP;
			}
		}

		guiGraphics.pose().popMatrix();
	}
}
