package com.tntsallin1client.debug;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.spawnoverlay.SpawnRiskCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Phase 5d: extra F3 block with the handful of values a player actually cares
 * about, in plain labels instead of vanilla's terse abbreviations - feedback
 * was "better description of what things do, or highlight what matters".
 * What it replaces or moves away is taken off the screen while it is on: the
 * game's own biome line, the game version, and the computer's specs (Java,
 * memory, CPU, display, graphics), which have their own page then
 * ({@link SystemInfoOverlay}).
 *
 * <p>1.21.8 has no debug screen entries to register yet (those came with 1.21.9) -
 * {@code DebugScreenOverlayMixin} calls this with the lists of lines the game
 * has just put together.
 */
public final class QuickInfo {
	private static final String HEADER = "§e";
	private static final String LABEL = "§a";
	private static final String RESET = "§r";
	/** The game starts its right column with this many lines about the computer, blank ones included (bytecode-checked). */
	private static final int SYSTEM_LINES = 10;

	private QuickInfo() {
	}

	public static void editLeftText(List<String> lines) {
		if (!ClientConfig.get().f3QuickInfoEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null || client.level == null) {
			return;
		}

		lines.removeIf(line -> line.startsWith("Minecraft ") || line.startsWith("Biome: "));

		lines.add("");
		lines.add(HEADER + "-- Quick Info --" + RESET);
		// A server can have the debug screen say less (the game then leaves out position, biome and
		// light itself) - what it hides stays hidden here too.
		if (!client.showOnlyReducedInfo()) {
			BlockPos pos = camera.blockPosition();
			int blockLight = client.level.getBrightness(LightLayer.BLOCK, pos);
			// Same classification as the spawn overlay: the old "block light < 8" rule is
			// pre-1.18 - since then any block light above the dimension's limit (0 in the
			// overworld) blocks hostile spawns, so light 7 claimed "can spawn" wrongly.
			String spawnHint = switch (SpawnRiskCalculator.classify(client.level, pos)) {
				case ALWAYS -> " " + Component.translatable("gui.tntsallin1client.f3_quick_info.spawn_always").getString();
				case NIGHT_ONLY -> " " + Component.translatable("gui.tntsallin1client.f3_quick_info.spawn_night").getString();
				case NEVER -> "";
			};
			lines.add(LABEL + "Position:" + RESET + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
			lines.add(LABEL + "Biome:" + RESET + " " + biomeName(client, pos));
			lines.add(LABEL + "Light here:" + RESET + " " + blockLight + spawnHint);
		}
		lines.add(LABEL + "FPS:" + RESET + " " + client.getFps());
	}

	public static void editRightText(List<String> lines) {
		if (!ClientConfig.get().f3QuickInfoEnabled || lines.size() < SYSTEM_LINES) {
			return;
		}
		lines.subList(0, SYSTEM_LINES).clear();
		// The blank line that set the computer's lines apart from what follows.
		if (!lines.isEmpty() && lines.get(0).isEmpty()) {
			lines.remove(0);
		}
	}

	private static String biomeName(Minecraft client, BlockPos pos) {
		Holder<Biome> biome = client.level.getBiome(pos);
		return biome.unwrap().map(QuickInfo::shortenBiomeId, unregistered -> "unknown");
	}

	/** Drops the "minecraft:" namespace for vanilla biomes; keeps it for modded/datapack ones to avoid ambiguity. */
	private static String shortenBiomeId(ResourceKey<Biome> key) {
		ResourceLocation id = key.location();
		return id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? id.getPath() : id.toString();
	}
}
