package com.tntsallin1client.resourcepack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.Optional;

/**
 * The text half of our own dark mode pack ({@code resourcepacks/dark-mode/build.py}). The pack makes
 * the container screens dark, but vanilla draws their labels ("Inventory", "Crafting", a villager's
 * trade list, the advancements title) in a hard-coded dark grey that no resource pack can change - on
 * a dark panel they would be unreadable.
 *
 * <p>So the pack says which colour those labels should have instead
 * ({@code assets/tntsallin1client/dark_mode.json}), and {@code GuiGraphicsMixin} swaps exactly
 * that one vanilla colour while a pack with that file is active. Every way of drawing GUI text ends in
 * the one method the mixin hooks, so no screen needs a patch of its own - the earlier attempt at a dark
 * inventory failed on exactly that, see Aktuelle_Phase.md (5r).
 */
public final class DarkModePack {
	private static final ResourceLocation MARKER = new ResourceLocation("tntsallin1client", "dark_mode.json");
	/** Vanilla's label colour, without alpha. */
	private static final int VANILLA_LABEL_COLOR = 0x404040;
	/** How often the marker is looked up again - picks up the pack being switched on or off. */
	private static final long RECHECK_MS = 1000;

	private static long lastCheck;
	private static @Nullable Integer labelColor;

	private DarkModePack() {
	}

	/** {@code color} unless it is vanilla's label colour and a dark mode pack is active - then that pack's label colour, same alpha. */
	public static int recolorLabel(int color) {
		if ((color & 0xFFFFFF) != VANILLA_LABEL_COLOR) {
			return color;
		}
		Integer replacement = labelColor();
		return replacement == null ? color : (color & 0xFF000000) | replacement;
	}

	private static @Nullable Integer labelColor() {
		long now = System.currentTimeMillis();
		if (now - lastCheck >= RECHECK_MS) {
			lastCheck = now;
			labelColor = readLabelColor();
		}
		return labelColor;
	}

	/** The active pack's {@code label_color} ("#rrggbb"), or null if no pack has the marker or it is malformed. */
	private static @Nullable Integer readLabelColor() {
		Optional<Resource> marker = Minecraft.getInstance().getResourceManager().getResource(MARKER);
		if (marker.isEmpty()) {
			return null;
		}
		try (Reader reader = marker.get().openAsReader()) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			String hex = json.get("label_color").getAsString();
			return hex.matches("#[0-9a-fA-F]{6}") ? Integer.parseInt(hex.substring(1), 16) : null;
		} catch (Exception e) {
			return null;
		}
	}
}
