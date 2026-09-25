package com.tntsallin1client.compat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Coexistence with the Essential mod (own user request), which people add as a custom mod:
 * <ul>
 *   <li>Its own menus (main menu buttons, social bar, ...) are drawn in vanilla style by its own UI
 *       framework, which our theming can't reach - so while it's installed the client design is off
 *       (see {@code ClientDesign.isClient}) and the whole game consistently looks like vanilla.</li>
 *   <li>Its keys (zoom, emotes, wardrobe, ...) overlap with ours - every Essential key is set to
 *       "not bound" once, the first time we see it. Keys already handled are remembered in
 *       {@link #FILE}, so a key the user binds again afterwards stays bound.</li>
 * </ul>
 *
 * <p>Two mod ids: {@code essential-container} is the jar in the mods folder, {@code essential} the
 * actual mod its loader downloads and adds at startup. Its keys live in {@code options.keyMappings}
 * under the {@code essential:general} category and are added before {@code options.txt} is read
 * (Essential's {@code MixinGameOptions} injects at {@code Options.load} HEAD), so they're all there
 * by {@code CLIENT_STARTED} and a saved "not bound" survives restarts.
 */
public final class EssentialCompat {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-essential.json");
	private static final String KEY_NAMESPACE = "essential";

	private static Boolean loaded;

	private EssentialCompat() {
	}

	public static synchronized boolean isLoaded() {
		if (loaded == null) {
			FabricLoader loader = FabricLoader.getInstance();
			loaded = loader.isModLoaded("essential") || loader.isModLoaded("essential-container");
		}
		return loaded;
	}

	/** Greys out a "Client Design" switch while Essential is installed, with a tooltip saying why. */
	public static Button lockDesignSwitch(Button button) {
		if (isLoaded()) {
			button.active = false;
			button.setTooltip(Tooltip.create(Component.translatable("gui.tntsallin1client.design.essential_locked")));
		}
		return button;
	}

	/** Sets every Essential key we haven't handled before to "not bound" - called once on {@code CLIENT_STARTED}. */
	public static void unbindNewKeys(Minecraft client) {
		if (!isLoaded()) {
			return;
		}
		Set<String> handled = loadHandledKeys();
		boolean changed = false;
		for (KeyMapping mapping : client.options.keyMappings) {
			if (!KEY_NAMESPACE.equals(mapping.getCategory().id().getNamespace()) || !handled.add(mapping.getName())) {
				continue;
			}
			if (!mapping.isUnbound()) {
				mapping.setKey(InputConstants.UNKNOWN);
			}
			changed = true;
		}
		if (changed) {
			KeyMapping.resetMapping();
			client.options.save();
			saveHandledKeys(handled);
			LOGGER.info("Essential detected - unbound its new keys");
		}
	}

	private static Set<String> loadHandledKeys() {
		Set<String> keys = new HashSet<>();
		if (!Files.exists(FILE)) {
			return keys;
		}
		try {
			JsonObject json = new Gson().fromJson(Files.readString(FILE), JsonObject.class);
			if (json != null && json.has("unboundKeys")) {
				for (JsonElement key : json.getAsJsonArray("unboundKeys")) {
					keys.add(key.getAsString());
				}
			}
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
			LOGGER.warn("Failed to read handled Essential keys from {}", FILE, e);
		}
		return keys;
	}

	private static void saveHandledKeys(Set<String> keys) {
		JsonArray array = new JsonArray();
		keys.stream().sorted().forEach(array::add);
		JsonObject json = new JsonObject();
		json.add("unboundKeys", array);
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, new Gson().toJson(json));
		} catch (IOException e) {
			LOGGER.warn("Failed to save handled Essential keys to {}", FILE, e);
		}
	}
}
