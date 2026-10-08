package com.tntsallin1client.resourcepack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "Dark Mode": dark inventories, buttons and hotbar, as a switch in the mod menu. The pictures are
 * our own resource pack (`resourcepacks/dark-mode`), which the launcher puts into the instance -
 * the switch only puts that pack into the game's list of active packs or takes it out again (see
 * {@link ClientPacks}).
 *
 * <p>One thing no pack can change: the game draws the screens' labels ("Inventory", "Crafting") in
 * a fixed dark grey, unreadable on a dark panel. The pack carries the color they should have
 * instead, and `FontMixin` swaps the one for the other while the pack is active - as in the other
 * versions.
 */
public final class DarkMode {
	/** How the pack's file name begins - the rest is the Minecraft version. */
	private static final String PACK_FILE = "TNT-Dark-Mode-";

	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	/** In the pack, and so only there while the pack is active. */
	private static final ResourceLocation COLOR_FILE = new ResourceLocation("tntsallin1client", "dark_mode.json");
	/** The color the game draws container labels in. */
	private static final int VANILLA_LABEL_COLOR = 0x404040;
	private static final int FALLBACK_LABEL_COLOR = 0xD0D0D0;
	private static final int RGB = 0xFFFFFF;
	/** How long what was found out about the active packs is believed - labels are drawn many times a frame. */
	private static final long RECHECK_MILLIS = 500L;

	private static long checkedAt;
	/** The labels' color while the pack is active, -1 while it isn't. */
	private static int labelColor = -1;

	private DarkMode() {
	}

	/** Whether the launcher has put the pack into this instance. */
	public static boolean isAvailable() {
		return ClientPacks.isAvailable(PACK_FILE);
	}

	public static boolean isEnabled() {
		return ClientPacks.isEnabled(PACK_FILE);
	}

	public static void setEnabled(boolean enabled) {
		ClientPacks.setEnabled(PACK_FILE, enabled);
		checkedAt = 0;
	}

	/** The color a text is drawn in: the pack's label color in place of the game's while the pack is active, otherwise the one handed in. */
	public static int textColor(int color) {
		if ((color & RGB) != VANILLA_LABEL_COLOR) {
			return color;
		}
		long now = System.currentTimeMillis();
		if (now - checkedAt > RECHECK_MILLIS) {
			checkedAt = now;
			labelColor = readLabelColor();
		}
		return labelColor < 0 ? color : (color & ~RGB) | labelColor;
	}

	private static int readLabelColor() {
		ResourceManager resources = Minecraft.getInstance().getResourceManager();
		if (!resources.hasResource(COLOR_FILE)) {
			return -1;
		}
		try (Resource resource = resources.getResource(COLOR_FILE);
				Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
			String hex = json.get("label_color").getAsString();
			if (hex.matches("#[0-9a-fA-F]{6}")) {
				return Integer.parseInt(hex.substring(1), 16);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to read the dark mode pack's label color.", e);
		}
		return FALLBACK_LABEL_COLOR;
	}
}
