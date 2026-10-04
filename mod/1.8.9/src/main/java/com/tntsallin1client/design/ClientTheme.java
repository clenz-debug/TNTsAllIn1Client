package com.tntsallin1client.design;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The launcher's seven theme colors (Settings -> Appearance), as ARGB - the client design draws in
 * exactly these, as in the Fabric versions. The launcher writes them to
 * `config/tntsallin1client-theme.json` before every launch (`clientDesignSync.ts`); the defaults
 * match its own for a game started any other way. Read once - the colors can't change while the
 * game runs.
 */
public final class ClientTheme {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String FILE_NAME = "tntsallin1client-theme.json";

	private static ClientTheme instance;

	public final int background1;
	public final int background2;
	public final int accent1;
	public final int accent2;
	public final int accent3;
	public final int accent4;
	public final int text;

	private ClientTheme(Map<?, ?> colors) {
		this.background1 = parse(colors, "background1", 0xFF000000);
		this.background2 = parse(colors, "background2", 0xFF1A1A1A);
		this.accent1 = parse(colors, "accent1", 0xFF3D3D3D);
		this.accent2 = parse(colors, "accent2", 0xFF4D4D4D);
		this.accent3 = parse(colors, "accent3", 0xFF5D5D5D);
		this.accent4 = parse(colors, "accent4", 0xFF6D6D6D);
		this.text = parse(colors, "text", 0xFFFFFFFF);
	}

	public static synchronized ClientTheme get() {
		if (instance == null) {
			instance = new ClientTheme(load());
		}
		return instance;
	}

	/** `color` with its alpha channel scaled by `alpha` (0..1) - for fades. */
	public static int withAlpha(int color, float alpha) {
		int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, alpha)));
		return (a << 24) | (color & 0x00FFFFFF);
	}

	private static Map<?, ?> load() {
		File file = new File(new File(MinecraftClient.getInstance().runDirectory, "config"), FILE_NAME);
		if (!file.isFile()) {
			return Collections.emptyMap();
		}
		try {
			Map<?, ?> colors = new Gson().fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), Map.class);
			return colors != null ? colors : Collections.emptyMap();
		} catch (IOException | JsonParseException | ClassCastException e) {
			LOGGER.warn("Failed to read theme colors from " + file, e);
			return Collections.emptyMap();
		}
	}

	/** "#rrggbb" -> opaque ARGB, `fallback` for anything missing or malformed. */
	private static int parse(Map<?, ?> colors, String key, int fallback) {
		Object raw = colors.get(key);
		if (!(raw instanceof String) || !((String) raw).matches("#[0-9a-fA-F]{6}")) {
			return fallback;
		}
		return 0xFF000000 | Integer.parseInt(((String) raw).substring(1), 16);
	}
}
