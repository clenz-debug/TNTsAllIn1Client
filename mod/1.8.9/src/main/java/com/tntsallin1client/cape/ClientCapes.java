package com.tntsallin1client.cape;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferedImageSkinProvider;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

/**
 * The capes players set in the launcher. Each player's active cape lies on the cape server under
 * their id - the same address the Fabric versions of the mod read (there through the bundled Cape
 * Provider mod). The first time a player is drawn their cape is requested in the background; until
 * it has arrived, and for players without one, the game shows whatever it would have shown anyway.
 *
 * <p>The image is 2:1 like the game's own capes, from 64x32 up to 2048x1024. The game's cape model
 * addresses its texture in fractions, so every one of those sizes fits it as it is.
 */
public final class ClientCapes {
	private static final String URL_FORMAT = "https://nxlc.de/tntcapes/%s.png";
	/** How long "this player has no cape" is believed before asking again - a cape set in the meantime shows up without a restart. */
	private static final long RETRY_MILLIS = 5 * 60 * 1000L;

	/** Only touched from the render thread. */
	private static final Map<UUID, Entry> ENTRIES = new HashMap<UUID, Entry>();

	private ClientCapes() {
	}

	/** The texture of the player's cape, or null while there is none (yet). Call from the render thread. */
	public static Identifier capeFor(UUID playerId) {
		Entry entry = ENTRIES.get(playerId);
		long now = System.currentTimeMillis();
		if (entry == null || (!entry.available && now - entry.requestedAt > RETRY_MILLIS)) {
			entry = request(playerId, now);
			ENTRIES.put(playerId, entry);
		}
		return entry.available ? entry.texture : null;
	}

	private static Entry request(UUID playerId, long now) {
		final Entry entry = new Entry(new Identifier("tntsallin1client", "capes/" + playerId), now);
		TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
		// Frees what an earlier, unanswered request left under the same name.
		textureManager.close(entry.texture);
		// The game's own downloading texture: fetches the image on a thread of its own and uploads it
		// the first time it is drawn. No file to cache in, no stand-in image - an unanswered request
		// (the server has no cape for this player) simply never becomes available.
		textureManager.loadTexture(entry.texture, new PlayerSkinTexture(null, String.format(URL_FORMAT, playerId), null, new BufferedImageSkinProvider() {
			@Override
			public BufferedImage parseSkin(BufferedImage image) {
				return image;
			}

			@Override
			public void setAvailable() {
				entry.available = true;
			}
		}));
		return entry;
	}

	private static final class Entry {
		final Identifier texture;
		final long requestedAt;
		/** Set from the download thread once the image is there. */
		volatile boolean available;

		Entry(Identifier texture, long requestedAt) {
			this.texture = texture;
			this.requestedAt = requestedAt;
		}
	}
}
