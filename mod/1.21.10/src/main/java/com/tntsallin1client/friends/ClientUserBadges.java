package com.tntsallin1client.friends;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The client logo in front of the nametag of every player who uses our client too - own wishlist
 * item ("Logo am Nametag, damit man andere Client-Nutzer erkennt"), drawn in that player's own
 * launcher theme colors (own follow-up: someone with a green theme shows a green logo to everyone,
 * someone with a red one a red logo).
 *
 * <p>Only the launcher talks to our backend, so this goes through the same file bridge as the friends
 * features: once a second we list the UUIDs of the tab list in {@link #FILE}, the launcher asks the
 * backend which of them use our client and in which colors ({@code POST /players/lookup}; every
 * launcher reports its own colors with its presence ping) and returns them as {@code clientUsers} in
 * the friends inbox, handed to {@link #setClientUsers} by {@link FriendsBridge}. Without our launcher
 * (or offline) there is no inbox, and nobody gets a logo.
 *
 * <p>The logo is text in our own font ({@code assets/tntsallin1client/font/client_badge.json}, made by
 * {@code designs/branding/make_client_badge.py}): white glyphs - octagon outline and the four beams - each
 * followed by a negative space so they all land on the same spot, each tinted in one theme color. The
 * font also has a glyph for the octagon's fill, but it isn't drawn (own user request after seeing it in
 * the game: the octagon stays hollow, the nametag's own background shows through).
 * {@code EntityRendererNameTagMixin} puts it in front of the name, so vanilla's
 * nametag rendering draws it like the name itself; a style color only replaces the RGB and keeps the
 * nametag's alpha, so it is still dimmed while sneaking and see-through behind walls.
 */
public final class ClientUserBadges {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-players.json");
	private static final int TICKS_BETWEEN_CHECKS = 20;
	private static final FontDescription FONT = new FontDescription.Resource(ResourceLocation.fromNamespaceAndPath("tntsallin1client", "client_badge"));
	// Glyphs of FONT: the six logo layers from U+E000 on, then "back to the layer's start" and the gap
	// before the name. Layer order = paint order = LAYER_COLOR_KEYS.
	private static final int FIRST_LAYER_CHAR = 0xE000;
	private static final String[] LAYER_COLOR_KEYS = {"background1", "background2", "accent1", "accent2", "accent3", "accent4"};
	private static final int BACK_CHAR = FIRST_LAYER_CHAR + LAYER_COLOR_KEYS.length;
	private static final int GAP_CHAR = BACK_CHAR + 1;
	/** Layer 0 is the octagon's fill, which is left out - drawing starts with its outline. */
	private static final int FIRST_DRAWN_LAYER = 1;
	/** The launcher's {@code DEFAULT_THEME_COLORS}, for players whose launcher hasn't reported colors yet. */
	private static final int[] DEFAULT_COLORS = {0x000000, 0x1A1A1A, 0x3D3D3D, 0x4D4D4D, 0x5D5D5D, 0x6D6D6D};

	private static int tickCounter;
	private static String lastWritten;
	/** Undashed UUID -> that player's logo colors (RGB, in LAYER_COLOR_KEYS order). */
	private static Map<String, int[]> clientUsers = Map.of();

	private ClientUserBadges() {
	}

	public static void tick(Minecraft mc) {
		if (++tickCounter < TICKS_BETWEEN_CHECKS) return;
		tickCounter = 0;
		if (!FriendsBridge.isActive()) return;

		// Sorted, so the same players always give the same file and nothing is rewritten needlessly.
		Set<String> uuids = new TreeSet<>();
		ClientPacketListener connection = mc.getConnection();
		if (connection != null) {
			for (PlayerInfo info : connection.getOnlinePlayers()) {
				uuids.add(undashed(info.getProfile().id()));
			}
		}
		JsonArray array = new JsonArray();
		uuids.forEach(array::add);
		JsonObject json = new JsonObject();
		json.add("uuids", array);

		String serialized = new Gson().toJson(json);
		if (Objects.equals(serialized, lastWritten)) return;
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, serialized);
			lastWritten = serialized;
		} catch (IOException e) {
			LOGGER.warn("Failed to write player list to {}", FILE, e);
		}
	}

	/** The launcher's latest answer: undashed UUID -> logo colors of the listed players who use our client. */
	static void setClientUsers(Map<String, int[]> undashedUuidToColors) {
		clientUsers = Map.copyOf(undashedUuidToColors);
	}

	/** One {@code clientUsers} entry's {@code colors} object ("#rrggbb" per key, or null) as RGB values;
	 * a missing or malformed color falls back to the default theme's. */
	static int[] parseColors(@Nullable JsonObject colors) {
		int[] parsed = DEFAULT_COLORS.clone();
		if (colors == null) return parsed;
		for (int layer = 0; layer < LAYER_COLOR_KEYS.length; layer++) {
			String key = LAYER_COLOR_KEYS[layer];
			if (colors.has(key) && colors.get(key).isJsonPrimitive()) {
				String hex = colors.get(key).getAsString();
				if (hex.matches("#[0-9a-fA-F]{6}")) {
					parsed[layer] = Integer.parseInt(hex.substring(1), 16);
				}
			}
		}
		return parsed;
	}

	/** {@code name} with the client logo in front of it if that player uses our client, else unchanged. */
	public static Component withBadge(UUID player, Component name) {
		int[] colors = clientUsers.get(undashed(player));
		if (colors == null) return name;

		MutableComponent badge = Component.empty().withStyle(style -> style.withFont(FONT));
		for (int layer = FIRST_DRAWN_LAYER; layer < colors.length; layer++) {
			badge.append(Component.literal(Character.toString(FIRST_LAYER_CHAR + layer)).withColor(colors[layer]));
			if (layer < colors.length - 1) {
				badge.append(Character.toString(BACK_CHAR));
			}
		}
		badge.append(Character.toString(GAP_CHAR));
		return Component.empty().append(badge).append(name);
	}

	private static String undashed(UUID uuid) {
		return uuid.toString().replace("-", "");
	}
}
