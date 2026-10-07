package com.tntsallin1client.cape;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.HttpTextureProcessor;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * The capes players set in the launcher. Each player's active cape lies on the cape server under
 * their id - the same address the other versions of the mod read. The first time a player is drawn
 * their cape is requested in the background; until it has arrived, and for players without one, the
 * game shows whatever it would have shown anyway.
 *
 * <p>The image is 2:1 like the game's own capes, from 64x32 up to 2048x1024. The game's cape model
 * addresses its texture in fractions, so every one of those sizes fits it as it is.
 *
 * <p>A worn elytra takes its look from the cape's image too - from a part of it the launcher's cape
 * converter only fills with one color. Such a cape leaves the elytra its own look (own user report:
 * a plain gray elytra); one whose elytra part is painted is used for it.
 */
public final class ClientCapes {
	private static final String URL_FORMAT = "https://nxlc.de/tntcapes/%s.png";
	/** How long "this player has no cape" is believed before asking again - a cape set in the meantime shows up without a restart. */
	private static final long RETRY_MILLIS = 5 * 60 * 1000L;

	/** The elytra's part of a cape image, in the 64x32 units the layout is given in. */
	private static final int LAYOUT_WIDTH = 64;
	private static final int ELYTRA_LEFT = 22;
	private static final int ELYTRA_TOP = 0;
	private static final int ELYTRA_WIDTH = 24;
	private static final int ELYTRA_HEIGHT = 22;

	/** Only touched from the render thread. */
	private static final Map<UUID, Entry> ENTRIES = new HashMap<UUID, Entry>();
	/** The cape textures - ours and the offline one - whose elytra part is one plain color. */
	private static final Set<ResourceLocation> PLAIN_ELYTRA = new HashSet<ResourceLocation>();

	private ClientCapes() {
	}

	/** The texture of the player's cape, or null while there is none (yet). Call from the render thread. */
	public static ResourceLocation capeFor(UUID playerId) {
		Entry entry = ENTRIES.get(playerId);
		long now = System.currentTimeMillis();
		if (entry == null || (!entry.available && now - entry.requestedAt > RETRY_MILLIS)) {
			entry = request(playerId, now);
			ENTRIES.put(playerId, entry);
		}
		return entry.available ? entry.texture : null;
	}

	/** The texture a worn elytra gets from this cape texture: the cape itself, or null (the elytra's own look) for one of ours without a painted elytra part. */
	public static ResourceLocation elytraTexture(ResourceLocation cape) {
		return cape != null && PLAIN_ELYTRA.contains(cape) ? null : cape;
	}

	/** Notes whether the elytra part of this cape image is painted. Call from the render thread. */
	public static void noteElytraPart(ResourceLocation cape, NativeImage image) {
		if (isElytraPlain(image)) {
			PLAIN_ELYTRA.add(cape);
		} else {
			PLAIN_ELYTRA.remove(cape);
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

	private static Entry request(UUID playerId, long now) {
		final Entry entry = new Entry(new ResourceLocation("tntsallin1client", "capes/" + playerId), now);
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		// Frees what an earlier, unanswered request left under the same name.
		textureManager.release(entry.texture);
		// The game's own downloading texture: fetches the image on a thread of its own and hands it
		// over on the game's. It insists on a stand-in image to start with - a default skin, never
		// shown: an unanswered request (the server has no cape for this player) simply never becomes
		// available.
		textureManager.register(entry.texture, new HttpTexture(downloadFile(playerId), String.format(URL_FORMAT, playerId), DefaultPlayerSkin.getDefaultSkin(),
				new HttpTextureProcessor() {
					@Override
					public NativeImage process(NativeImage image) {
						noteElytraPart(entry.texture, image);
						return image;
					}

					@Override
					public void onTextureDownloaded() {
						entry.available = true;
					}
				}));
		return entry;
	}

	/**
	 * Where the download is put before it is read. Not optional in this version: without a file the
	 * game reads the image only after it has closed the connection it comes through. A file left
	 * from an earlier request is removed - the game would take it as the answer without asking the
	 * server, and a cape changed in the launcher would never show up.
	 */
	private static File downloadFile(UUID playerId) {
		File file = new File(new File(System.getProperty("java.io.tmpdir"), "tntsallin1client-capes"), playerId + ".png");
		file.delete();
		file.deleteOnExit();
		return file;
	}

	private static final class Entry {
		final ResourceLocation texture;
		final long requestedAt;
		/** Set once the image is there. */
		volatile boolean available;

		Entry(ResourceLocation texture, long requestedAt) {
			this.texture = texture;
			this.requestedAt = requestedAt;
		}
	}
}
