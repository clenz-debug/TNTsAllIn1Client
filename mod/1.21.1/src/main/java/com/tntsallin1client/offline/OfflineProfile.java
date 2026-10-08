package com.tntsallin1client.offline;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * The local player's own skin and cape in the launcher's offline mode (own user request). Minecraft
 * normally downloads both, so without internet the player would be a default Steve/Alex without a
 * cape. The launcher keeps a copy from the last online launch and, for an offline launch only,
 * hands it over in {@link #FILE} plus the PNGs in {@link #DIR} ({@code offlineProfile.ts});
 * {@code SkinManagerMixin} then returns this skin for the local player instead of looking it up.
 *
 * <p>Marked secure like the newer versions' - it only ever applies to the local player's own profile.
 */
public final class OfflineProfile {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-offline.json");
	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-offline");

	private static boolean loaded;
	private static boolean offline;
	private static boolean hasSkin;
	private static boolean hasCape;
	private static PlayerSkin.Model model = PlayerSkin.Model.WIDE;
	private static CompletableFuture<PlayerSkin> skin;

	private OfflineProfile() {
	}

	/** Whether the launcher started the game in offline mode. */
	public static synchronized boolean isOfflineLaunch() {
		load();
		return offline;
	}

	/** The local player's offline skin, or {@code null} when this isn't an offline launch. */
	public static synchronized CompletableFuture<PlayerSkin> localSkin() {
		load();
		if (!offline) {
			return null;
		}
		if (skin == null) {
			// Textures may only be registered on the render thread.
			skin = Minecraft.getInstance().submit(OfflineProfile::createSkin);
		}
		return skin;
	}

	private static void load() {
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
			model = json.has("model") && "slim".equals(json.get("model").getAsString()) ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
			LOGGER.warn("Failed to read offline profile from {}", FILE, e);
		}
	}

	private static PlayerSkin createSkin() {
		PlayerSkin fallback = DefaultPlayerSkin.get(Minecraft.getInstance().getUser().getProfileId());
		ResourceLocation body = hasSkin ? registerSkin(fallback) : null;
		ResourceLocation cape = hasCape ? registerCape() : null;
		if (body == null) {
			return new PlayerSkin(fallback.texture(), null, cape, null, fallback.model(), true);
		}
		return new PlayerSkin(body, null, cape, null, model, true);
	}

	/**
	 * The game's own skin texture, pointed at the launcher's copy as its "already downloaded" file:
	 * it then never asks the internet and gives old 64x32 skins the same conversion a downloaded
	 * skin gets. Until the picture is read it shows {@code fallback}.
	 */
	private static ResourceLocation registerSkin(PlayerSkin fallback) {
		Path file = DIR.resolve("skin.png");
		if (!Files.isRegularFile(file)) {
			LOGGER.warn("Offline skin {} is missing", file);
			return null;
		}
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "offline/skin");
		Minecraft.getInstance().getTextureManager().register(id, new HttpTexture(file.toFile(), "", fallback.texture(), true, null));
		return id;
	}

	private static ResourceLocation registerCape() {
		Path file = DIR.resolve("cape.png");
		try (InputStream in = Files.newInputStream(file)) {
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "offline/cape");
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(NativeImage.read(in)));
			return id;
		} catch (IOException e) {
			LOGGER.warn("Failed to load offline cape from {}", file, e);
			return null;
		}
	}
}
