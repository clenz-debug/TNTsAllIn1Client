package com.tntsallin1client.debug;

import java.util.Iterator;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.spawnoverlay.SpawnRisk;
import com.tntsallin1client.spawnoverlay.SpawnRiskCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;

/**
 * "F3 Quick Info": an extra block on the debug screen with the handful of values a player actually
 * looks for, in plain words. What it replaces or moves away is taken off the screen while it is on:
 * the game's own biome line, the game version, and the computer's specs (Java, memory, CPU,
 * display, graphics), which have their own page then ({@link SystemInfoHud}). Called from
 * `DebugScreenOverlayMixin` with the lists of lines the game has just put together.
 */
public final class QuickInfo {
	private static final String HEADER = "§e";
	private static final String LABEL = "§a";
	private static final String RESET = "§r";
	/** The game starts its right column with this many lines about the computer, blank ones included (bytecode-checked). */
	private static final int SYSTEM_LINES = 9;

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

		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
			String line = iterator.next();
			if (line.startsWith("Minecraft ") || line.startsWith("Biome: ")) {
				iterator.remove();
			}
		}

		lines.add("");
		lines.add(HEADER + "-- Quick Info --" + RESET);
		// A server can have the debug screen say less (the game then leaves out position, biome and
		// light itself) - what it hides stays hidden here too.
		if (!client.showOnlyReducedInfo()) {
			BlockPos pos = new BlockPos(camera);
			lines.add(LABEL + "Position:" + RESET + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
			lines.add(LABEL + "Biome:" + RESET + " " + client.level.getBiome(pos).getName().getString());
			lines.add(LABEL + "Light here:" + RESET + " " + client.level.getBrightness(LightLayer.BLOCK, pos) + spawnHint(client, pos));
		}
		lines.add(LABEL + "FPS:" + RESET + " " + Minecraft.getAverageFps());
	}

	/** Same classification as the light level overlay. */
	private static String spawnHint(Minecraft client, BlockPos pos) {
		SpawnRisk risk = SpawnRiskCalculator.classify(client.level, pos);
		if (risk == SpawnRisk.ALWAYS) {
			return " " + I18n.get("gui.tntsallin1client.f3_quick_info.spawn_always");
		}
		if (risk == SpawnRisk.NIGHT_ONLY) {
			return " " + I18n.get("gui.tntsallin1client.f3_quick_info.spawn_night");
		}
		return "";
	}

	public static void editRightText(List<String> lines) {
		if (ClientConfig.get().f3QuickInfoEnabled && lines.size() >= SYSTEM_LINES) {
			lines.subList(0, SYSTEM_LINES).clear();
		}
	}
}
