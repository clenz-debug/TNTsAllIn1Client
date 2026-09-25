package com.tntsallin1client.offline;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.mixin.SkinTextureDownloaderInvoker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The local player's own skin and cape in the launcher's offline mode (own user request). Minecraft
 * normally downloads both, so without internet the player would be a default Steve/Alex without a
 * cape. The launcher keeps a copy from the last online launch and, for an offline launch only,
 * hands it over in {@link #FILE} plus the PNGs in {@link #DIR} ({@code offlineProfile.ts});
 * {@code SkinManagerMixin} then returns this skin for the local player instead of looking it up.
 *
 * <p>Marked secure on purpose: {@code SkinManager.createLookup(profile, true)} would otherwise throw
 * it away again for the default skin. It only ever applies to the local player's own profile.
 */
public final class OfflineProfile {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-offline.json");
	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-offline");

	private static boolean loaded;
	private static boolean offline;
	private static boolean hasSkin;
	private static boolean hasCape;
	private static PlayerModelType model = PlayerModelType.WIDE;
	private static CompletableFuture<Optional<PlayerSkin>> skin;

	private OfflineProfile() {
	}

	/** Whether the launcher started the game in offline mode. */
	public static synchronized boolean isOfflineLaunch() {
		load();
		return offline;
	}

	/** The local player's offline skin, or {@code null} when this isn't an offline launch. */
	public static synchronized CompletableFuture<Optional<PlayerSkin>> localSkin() {
		load();
		if (!offline) {
			return null;
		}
		if (skin == null) {
			// Textures may only be registered on the render thread.
			skin = Minecraft.getInstance().submit(OfflineProfile::createSkin).thenApply(Optional::of);
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
			model = json.has("model") && "slim".equals(json.get("model").getAsString()) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
			LOGGER.warn("Failed to read offline profile from {}", FILE, e);
		}
	}

	private static PlayerSkin createSkin() {
		PlayerSkin fallback = DefaultPlayerSkin.get(Minecraft.getInstance().getUser().getProfileId());
		ClientAsset.Texture body = hasSkin ? register("skin", true) : null;
		ClientAsset.Texture cape = hasCape ? register("cape", false) : null;
		if (body == null) {
			return new PlayerSkin(fallback.body(), cape, null, fallback.model(), true);
		}
		return new PlayerSkin(body, cape, null, model, true);
	}

	private static ClientAsset.Texture register(String name, boolean isSkin) {
		Path file = DIR.resolve(name + ".png");
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage image = NativeImage.read(in);
			if (isSkin) {
				// Old 64x32 skins get the same conversion vanilla's own skin download applies.
				image = SkinTextureDownloaderInvoker.tntsallin1client$processLegacySkin(image, file.toString());
				if (image == null) {
					return null;
				}
			}
			Identifier id = Identifier.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "offline/" + name);
			NativeImage finalImage = image;
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "offline " + name, finalImage));
			return new ClientAsset.ResourceTexture(id, id);
		} catch (IOException e) {
			LOGGER.warn("Failed to load offline {} from {}", name, file, e);
			return null;
		}
	}
}
