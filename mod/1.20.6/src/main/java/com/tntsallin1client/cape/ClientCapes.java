package com.tntsallin1client.cape;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The capes players set in the launcher. Each player's active cape lies on the cape server under
 * their id - the same address the other versions of the mod read. There is no Cape Provider mod for
 * this Minecraft version, so the mod fetches them itself: the first time a player is drawn their cape
 * is requested in the background; until it has arrived, and for players without one, the game shows
 * whatever it would have shown anyway.
 *
 * <p>The image is 2:1 like the game's own capes, from 64x32 up to 2048x1024. The game's cape model
 * addresses its texture in fractions, so every one of those sizes fits it as it is.
 *
 * <p>A worn elytra takes its look from the cape's image too - from a part of it the launcher's cape
 * converter only fills with one color. Such a cape leaves the elytra its own look (own user report:
 * a plain gray elytra); one whose elytra part is painted is used for it.
 */
public final class ClientCapes {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String URL_FORMAT = "https://nxlc.de/tntcapes/%s.png";
	/** How long "this player has no cape" is believed before asking again - a cape set in the meantime shows up without a restart. */
	private static final long RETRY_MILLIS = 5 * 60 * 1000L;
	private static final int TIMEOUT_MILLIS = 10_000;
	/** Far above the largest cape the launcher uploads - a guard against a broken answer. */
	private static final int MAX_FILE_BYTES = 8 * 1024 * 1024;

	/** The elytra's part of a cape image, in the 64x32 units the layout is given in. */
	private static final int LAYOUT_WIDTH = 64;
	private static final int ELYTRA_LEFT = 22;
	private static final int ELYTRA_TOP = 0;
	private static final int ELYTRA_WIDTH = 24;
	private static final int ELYTRA_HEIGHT = 22;

	/** Only touched from the render thread. */
	private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
	/** Our cape textures whose elytra part is one plain color. Only touched from the render thread. */
	private static final Set<ResourceLocation> PLAIN_ELYTRA = new HashSet<>();

	private ClientCapes() {
	}

	/** {@code skin} wearing the player's cape from our server, or {@code skin} itself while there is none (yet). Call from the render thread. */
	public static PlayerSkin withCape(UUID playerId, PlayerSkin skin) {
		Entry entry = ENTRIES.get(playerId);
		long now = System.currentTimeMillis();
		if (entry == null || (!entry.available && now - entry.requestedAt > RETRY_MILLIS)) {
			entry = request(playerId, now);
			ENTRIES.put(playerId, entry);
		}
		if (!entry.available) {
			return skin;
		}
		// Asked several times a frame - the same skin gets the same answer without a new object.
		if (entry.base != skin) {
			entry.base = skin;
			entry.dressed = new PlayerSkin(skin.texture(), skin.textureUrl(), entry.texture, skin.elytraTexture(), skin.model(), skin.secure());
		}
		return entry.dressed;
	}

	/** The texture a worn elytra gets from this cape texture: the cape itself, or null (the elytra's own look) for one of ours without a painted elytra part. */
	public static ResourceLocation elytraTexture(ResourceLocation cape) {
		return cape != null && PLAIN_ELYTRA.contains(cape) ? null : cape;
	}

	private static Entry request(UUID playerId, long now) {
		Entry entry = new Entry(new ResourceLocation("tntsallin1client", "capes/" + playerId), now);
		Minecraft minecraft = Minecraft.getInstance();
		CompletableFuture.supplyAsync(() -> download(playerId), Util.ioPool()).thenAcceptAsync(file -> {
			if (file == null) {
				return;
			}
			try {
				NativeImage image = NativeImage.read(file);
				if (isElytraPlain(image)) {
					PLAIN_ELYTRA.add(entry.texture);
				} else {
					PLAIN_ELYTRA.remove(entry.texture);
				}
				// Replaces (and frees) what an earlier request left under the same name.
				minecraft.getTextureManager().register(entry.texture, new DynamicTexture(image));
				entry.available = true;
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Failed to read the cape of {}", playerId, e);
			}
		}, minecraft);
		return entry;
	}

	/** The cape's image file, or null if the player has none or the server can't be reached. */
	private static byte[] download(UUID playerId) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) URI.create(String.format(URL_FORMAT, playerId)).toURL().openConnection();
			connection.setConnectTimeout(TIMEOUT_MILLIS);
			connection.setReadTimeout(TIMEOUT_MILLIS);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			try (InputStream stream = connection.getInputStream()) {
				byte[] file = stream.readNBytes(MAX_FILE_BYTES + 1);
				return file.length > MAX_FILE_BYTES ? null : file;
			}
		} catch (IOException | RuntimeException e) {
			// No connection (or an offline launch): simply no cape, asked again later.
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static boolean isElytraPlain(NativeImage image) {
		int scale = image.getWidth() / LAYOUT_WIDTH;
		int right = Math.min(image.getWidth(), (ELYTRA_LEFT + ELYTRA_WIDTH) * scale);
		int bottom = Math.min(image.getHeight(), (ELYTRA_TOP + ELYTRA_HEIGHT) * scale);
		if (scale < 1 || right <= ELYTRA_LEFT * scale || bottom <= ELYTRA_TOP * scale) {
			// Not a cape image as we know it - left to the game.
			return false;
		}
		int first = image.getPixelRGBA(ELYTRA_LEFT * scale, ELYTRA_TOP * scale);
		for (int y = ELYTRA_TOP * scale; y < bottom; y++) {
			for (int x = ELYTRA_LEFT * scale; x < right; x++) {
				if (image.getPixelRGBA(x, y) != first) {
					return false;
				}
			}
		}
		return true;
	}

	private static final class Entry {
		final ResourceLocation texture;
		final long requestedAt;
		/** Set on the render thread once the image is there. */
		boolean available;
		/** The skin last asked about and that skin with this cape. */
		PlayerSkin base;
		PlayerSkin dressed;

		Entry(ResourceLocation texture, long requestedAt) {
			this.texture = texture;
			this.requestedAt = requestedAt;
		}
	}
}
