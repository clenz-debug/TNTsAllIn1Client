package com.tntsallin1client.design;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * The launcher's seven theme colors (Settings -> Erscheinungsbild), as ARGB - the client design
 * draws the title screen, logo and Client Mods menu in exactly these (own user request: "launcher
 * theme, das was der user eingestellt hat"). The launcher writes them to {@link #FILE} before every
 * launch ({@code clientDesignSync.ts}); defaults match its own {@code DEFAULT_THEME_COLORS} for a
 * game started any other way. Read once - the colors can't change while the game runs.
 */
public final class ClientTheme {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-theme.json");

	private static ClientTheme instance;

	public final int background1;
	public final int background2;
	public final int accent1;
	public final int accent2;
	public final int accent3;
	public final int accent4;
	public final int text;

	private ClientTheme(Map<String, String> colors) {
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

	/** {@code color} with its alpha channel scaled by {@code alpha} (0..1) - for fades. */
	public static int withAlpha(int color, float alpha) {
		int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, alpha)));
		return (a << 24) | (color & 0x00FFFFFF);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> load() {
		if (!Files.exists(FILE)) {
			return Map.of();
		}
		try {
			Map<String, String> colors = new Gson().fromJson(Files.readString(FILE), Map.class);
			return colors != null ? colors : Map.of();
		} catch (IOException | JsonParseException | ClassCastException e) {
			LOGGER.warn("Failed to read theme colors from {}", FILE, e);
			return Map.of();
		}
	}

	/** "#rrggbb" -> opaque ARGB, {@code fallback} for anything missing or malformed. */
	private static int parse(Map<String, String> colors, String key, int fallback) {
		Object raw = colors.get(key);
		if (!(raw instanceof String hex) || !hex.matches("#[0-9a-fA-F]{6}")) {
			return fallback;
		}
		return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
	}
}
