package com.tntsallin1client.resourcepack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.ResourcePackLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "Dark Mode": dark inventories, buttons and hotbar, as a switch in the mod menu. The pictures are
 * a resource pack the launcher builds into this instance from the game's own files
 * (`legacyDarkModePack.ts` in the launcher) - the switch only puts that pack into the game's list
 * of active packs or takes it out again (see {@link LauncherPacks}).
 *
 * <p>One thing no pack can change: the game draws the screens' labels ("Inventory", "Crafting") in
 * a fixed dark grey, unreadable on a dark panel. The pack carries the color they should have
 * instead, and `TextRendererMixin` swaps the one for the other while the pack is active - as in the
 * Fabric versions.
 */
public final class DarkMode {
	/** The pack's folder in `resourcepacks/` - `LEGACY_DARK_MODE_PACK_NAME` in the launcher. */
	public static final String PACK_NAME = "TNT Dark Mode";

	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String COLOR_FILE = "assets/tntsallin1client/dark_mode.json";
	/** The color the game draws container labels in. */
	private static final int VANILLA_LABEL_COLOR = 0x404040;
	private static final int FALLBACK_LABEL_COLOR = 0xD0D0D0;
	private static final int RGB = 0xFFFFFF;

	/** The list of active packs {@link #labelColor} was worked out for - the game makes a new list whenever it changes. */
	private static List<ResourcePackLoader.Entry> checkedPacks;
	/** The labels' color while the pack is active, -1 while it isn't. */
	private static int labelColor = -1;

	private DarkMode() {
	}

	/** Whether the launcher has put the pack into this instance. */
	public static boolean isAvailable() {
		return LauncherPacks.isAvailable(PACK_NAME);
	}

	public static boolean isEnabled() {
		return LauncherPacks.isEnabled(PACK_NAME);
	}

	public static void setEnabled(boolean enabled) {
		LauncherPacks.setEnabled(PACK_NAME, enabled);
	}

	/** The color a text is drawn in: the pack's label color in place of the game's while the pack is active, otherwise the one handed in. */
	public static int textColor(int color) {
		if ((color & RGB) != VANILLA_LABEL_COLOR) {
			return color;
		}
		List<ResourcePackLoader.Entry> packs = MinecraftClient.getInstance().getResourcePackLoader().getSelectedResourcePacks();
		if (packs != checkedPacks) {
			checkedPacks = packs;
			labelColor = isEnabled() ? readLabelColor() : -1;
		}
		return labelColor < 0 ? color : (color & ~RGB) | labelColor;
	}

	private static int readLabelColor() {
		File file = new File(LauncherPacks.folder(PACK_NAME), COLOR_FILE);
		try {
			JsonObject json = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
			String hex = json.get("label_color").getAsString();
			if (hex.matches("#[0-9a-fA-F]{6}")) {
				return Integer.parseInt(hex.substring(1), 16);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to read the dark mode pack's label color from " + file + ".", e);
		}
		return FALLBACK_LABEL_COLOR;
	}
}
