package com.tntsallin1client.offline;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.cape.ClientCapes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * The local player's own skin and cape in the launcher's offline mode (own user request). Minecraft
 * normally downloads both, so without internet the player would be a default Steve/Alex without a
 * cape. The launcher keeps a copy from the last online launch and, for an offline launch only,
 * hands it over in {@link #FILE} plus the PNGs in {@link #DIR} ({@code offlineProfile.ts});
 * {@code PlayerInfoMixin} then gives these out for the local player instead of what the game would
 * look up. This version has no skin record yet - skin, cape and arm width are asked for one by one.
 */
public final class OfflineProfile {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-offline.json");
	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-offline");
	/** The two arm widths, as the game names them. */
	private static final String SLIM_MODEL = "slim";
	private static final String WIDE_MODEL = "default";

	private static boolean loaded;
	private static boolean offline;
	private static boolean hasSkin;
	private static boolean hasCape;
	private static boolean slim;
	/** Made the first time they are asked for - textures can only be made on the render thread. */
	private static ResourceLocation skin;
	private static ResourceLocation cape;
	private static boolean texturesLoaded;

	private OfflineProfile() {
	}

	/** Whether the launcher started the game in offline mode. */
	public static synchronized boolean isOfflineLaunch() {
		load();
		return offline;
	}

	/** The local player's offline skin, or null when there is none to use. Call from the render thread. */
	public static ResourceLocation skin() {
		loadTextures();
		return skin;
	}

	/** The local player's offline cape, or null when there is none to use. Call from the render thread. */
	public static ResourceLocation cape() {
		loadTextures();
		return cape;
	}

	/** The arm width that goes with {@link #skin()}, as the game names it - null while that skin isn't used. */
	public static String model() {
		return skin() == null ? null : slim ? SLIM_MODEL : WIDE_MODEL;
	}

	private static synchronized void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			JsonObject json = new Gson().fromJson(Files.readString(FILE), JsonObject.class);
			if (json == null) {
				return;
			}
			offline = json.has("offline") && json.get("offline").getAsBoolean();
			hasSkin = json.has("skin") && json.get("skin").getAsBoolean();
			hasCape = json.has("cape") && json.get("cape").getAsBoolean();
			slim = json.has("model") && SLIM_MODEL.equals(json.get("model").getAsString());
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
			LOGGER.warn("Failed to read offline profile from {}", FILE, e);
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
			skin = registerSkin();
		}
		if (hasCape) {
			cape = registerCape();
		}
	}

	/**
	 * The game's own skin texture, pointed at the launcher's copy as its "already downloaded" file:
	 * it then never asks the internet and gives old 64x32 skins the same conversion a downloaded
	 * skin gets. Until the picture is read it shows the default skin.
	 */
	private static ResourceLocation registerSkin() {
		Path file = DIR.resolve("skin.png");
		if (!Files.isRegularFile(file)) {
			LOGGER.warn("Offline skin {} is missing", file);
			return null;
		}
		// This version's user id can be missing (an unreadable one comes back as null)
		UUID playerId = Minecraft.getInstance().getUser().getGameProfile().getId();
		ResourceLocation fallback = playerId != null ? DefaultPlayerSkin.getDefaultSkin(playerId) : DefaultPlayerSkin.getDefaultSkin();
		ResourceLocation id = new ResourceLocation(TNTsAllIn1ClientMod.MOD_ID, "offline/skin");
		Minecraft.getInstance().getTextureManager().register(id, new HttpTexture(file.toFile(), "", fallback, true, null));
		return id;
	}

	private static ResourceLocation registerCape() {
		Path file = DIR.resolve("cape.png");
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage image = NativeImage.read(in);
			ResourceLocation id = new ResourceLocation(TNTsAllIn1ClientMod.MOD_ID, "offline/cape");
			ClientCapes.noteElytraPart(id, image);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
			return id;
		} catch (IOException e) {
			LOGGER.warn("Failed to load offline cape from {}", file, e);
			return null;
		}
	}
}
