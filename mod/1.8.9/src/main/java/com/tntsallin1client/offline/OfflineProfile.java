package com.tntsallin1client.offline;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DownloadedSkinParser;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The local player's own skin and cape in the launcher's offline mode, as in the Fabric versions.
 * The game normally downloads both, so without internet the player would be a default Steve or Alex
 * without a cape. The launcher keeps a copy from the last online start and, for an offline start
 * only, hands it over in {@link #FILE} plus the pictures in {@link #DIR}; `AbstractClientPlayerEntityMixin`
 * then gives these out for the local player instead of what the game would look up.
 */
public final class OfflineProfile {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String FILE = "config/tntsallin1client-offline.json";
	private static final String DIR = "config/tntsallin1client-offline";
	/** The two arm widths, as the game names them. */
	private static final String SLIM_MODEL = "slim";
	private static final String WIDE_MODEL = "default";

	private static boolean loaded;
	private static boolean offline;
	private static boolean hasSkin;
	private static boolean hasCape;
	private static boolean slim;
	/** Made the first time they are asked for - textures can only be made on the render thread. */
	private static Identifier skin;
	private static Identifier cape;
	private static boolean texturesLoaded;

	private OfflineProfile() {
	}

	/** Whether the launcher started the game in its offline mode. */
	public static boolean isOfflineLaunch() {
		load();
		return offline;
	}

	/** The local player's offline skin, or null when there is none to use. Call from the render thread. */
	public static Identifier skin() {
		loadTextures();
		return skin;
	}

	/** The local player's offline cape, or null when there is none to use. Call from the render thread. */
	public static Identifier cape() {
		loadTextures();
		return cape;
	}

	/** The arm width that goes with {@link #skin()}, as the game names it - null while that skin isn't used. */
	public static String model() {
		return skin() == null ? null : slim ? SLIM_MODEL : WIDE_MODEL;
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		File file = new File(MinecraftClient.getInstance().runDirectory, FILE);
		if (!file.isFile()) {
			return;
		}
		try {
			JsonObject json = new Gson().fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), JsonObject.class);
			if (json == null) {
				return;
			}
			offline = json.has("offline") && json.get("offline").getAsBoolean();
			hasSkin = json.has("skin") && json.get("skin").getAsBoolean();
			hasCape = json.has("cape") && json.get("cape").getAsBoolean();
			slim = json.has("model") && SLIM_MODEL.equals(json.get("model").getAsString());
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to read the offline profile from " + file + ".", e);
		}
	}

	private static void loadTextures() {
		if (texturesLoaded) {
			return;
		}
		texturesLoaded = true;
		load();
		if (!offline) {
			return;
		}
		if (hasSkin) {
			skin = register("skin", true);
		}
		if (hasCape) {
			cape = register("cape", false);
		}
	}

	private static Identifier register(String name, boolean isSkin) {
		File file = new File(MinecraftClient.getInstance().runDirectory, DIR + "/" + name + ".png");
		try {
			BufferedImage image = ImageIO.read(file);
			if (image != null && isSkin) {
				// The same treatment a downloaded skin gets: an old 64x32 skin is brought to today's layout.
				image = new DownloadedSkinParser().parseSkin(image);
			}
			if (image == null) {
				return null;
			}
			Identifier id = new Identifier("tntsallin1client", "offline/" + name);
			MinecraftClient.getInstance().getTextureManager().loadTexture(id, new NativeImageBackedTexture(image));
			return id;
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to load the offline " + name + " from " + file + ".", e);
			return null;
		}
	}
}
