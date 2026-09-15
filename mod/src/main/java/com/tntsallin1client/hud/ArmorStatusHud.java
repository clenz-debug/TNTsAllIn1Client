package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

	private ArmorStatusHud() {
	}

	public record Entry(ArmorStatusSlot slot, ItemStack stack, String text, int color) {
	}

	/** Whether a slot's display is turned on - absent from the map (nothing toggled off yet) counts as enabled, same convention as {@link ClientConfig#keystrokesKeyEnabled}. */
	public static boolean isSlotEnabled(ClientConfig config, ArmorStatusSlot slot) {
		return config.armorStatusSlotEnabled.getOrDefault(slot.name(), true);
	}

	/** Entries for every enabled slot that's actually holding/wearing something, in {@link ArmorStatusSlot} order. */
	public static List<Entry> buildEntries(ClientConfig config, LocalPlayer player) {
		List<Entry> entries = new ArrayList<>(ArmorStatusSlot.values().length);
		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
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
				? (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()
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
		List<Entry> entries = new ArrayList<>(ArmorStatusSlot.values().length);
		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
			if (isSlotEnabled(config, slot)) {
				entries.add(buildMaxEntry(config, slot));
			}
		}
		return entries;
	}

	/** The one-slot version of {@link #buildMaxEntries}, for the per-slot layout mode's own editor bounds. */
	public static Entry buildMaxEntry(ClientConfig config, ArmorStatusSlot slot) {
		String label = Component.translatable("gui.tntsallin1client.armor_status_slot." + slot.name().toLowerCase(Locale.ROOT)).getString();
		String text = config.armorStatusShowName ? label + ": " + MAX_DURABILITY_PLACEHOLDER : MAX_DURABILITY_PLACEHOLDER;
		return new Entry(slot, ItemStack.EMPTY, text, config.armorStatusColor);
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
	public static void drawRow(GuiGraphics guiGraphics, Font font, Entry entry, int localX, int localY, boolean showIcon) {
		if (showIcon) {
			guiGraphics.renderItem(entry.stack(), localX, localY);
			int textY = localY + (ICON_SIZE - font.lineHeight) / 2;
			guiGraphics.drawString(font, entry.text(), localX + ICON_SIZE + ICON_GAP, textY, entry.color());
		} else {
			guiGraphics.drawString(font, entry.text(), localX, localY, entry.color());
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

	/** Draws every entry as one bundled block, translated/scaled onto the given origin by the caller's own pose-stack push. */
	public static void drawBundled(GuiGraphics guiGraphics, Font font, ClientConfig config, List<Entry> entries, float x, float y, float scale) {
		boolean showIcon = config.armorStatusShowIcon;
		int rowHeight = contentHeight(font, showIcon);

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(scale);

		int cursor = 0;
		for (Entry entry : entries) {
			if (config.armorStatusBundledDirection == ArmorStatusDirection.VERTICAL) {
				drawRow(guiGraphics, font, entry, 0, cursor, showIcon);
				cursor += rowHeight + GAP;
			} else {
				drawRow(guiGraphics, font, entry, cursor, 0, showIcon);
				cursor += contentWidth(font, entry.text(), showIcon) + GAP;
			}
		}

		guiGraphics.pose().popMatrix();
	}
}
