package com.tntsallin1client.debug;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Phase 5d: extra F3 block with the handful of values a player actually cares
 * about, in plain labels instead of vanilla's terse abbreviations - feedback
 * was "better description of what things do, or highlight what matters".
 * Registered through vanilla's own {@code DebugScreenEntries.register(...)}
 * extension point rather than a mixin, so it doesn't touch (and can't break on)
 * Mojang's own debug screen code.
 */
public class QuickInfoDebugEntry implements DebugScreenEntry {
	private static final Identifier GROUP = Identifier.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "quick_info");
	private static final String HEADER = "§e";
	private static final String LABEL = "§a";
	private static final String RESET = "§r";

	@Override
	public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
		if (!ClientConfig.get().f3QuickInfoEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null || client.level == null) {
			return;
		}

		BlockPos pos = camera.blockPosition();
		int blockLight = client.level.getBrightness(LightLayer.BLOCK, pos);
		String spawnHint = blockLight < 8 ? " (mobs can spawn here)" : "";

		debugScreenDisplayer.addToGroup(GROUP, List.of(
				HEADER + "-- Quick Info --" + RESET,
				LABEL + "Position:" + RESET + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
				LABEL + "Biome:" + RESET + " " + biomeName(client, pos),
				LABEL + "Light here:" + RESET + " " + blockLight + spawnHint,
				LABEL + "FPS:" + RESET + " " + client.getFps()
		));
	}

	@Override
	public boolean isAllowed(boolean reducedInfo) {
		return true;
	}

	/**
	 * Hides the vanilla entries this feature replaces/relocates from the main F3
	 * screen: biome (superseded by our cleaner line above) and system specs /
	 * game version (moved to the separate F3+S page, see {@link SystemInfoOverlay}).
	 * Reversible - restores vanilla's own default visibility when turned off.
	 * Call whenever {@code f3QuickInfoEnabled} changes, not just once at startup.
	 */
	public static void applyVanillaEntryVisibility(Minecraft client) {
		DebugScreenEntryStatus status = ClientConfig.get().f3QuickInfoEnabled
				? DebugScreenEntryStatus.NEVER
				: DebugScreenEntryStatus.IN_OVERLAY;
		client.debugEntries.setStatus(DebugScreenEntries.BIOME, status);
		client.debugEntries.setStatus(DebugScreenEntries.SYSTEM_SPECS, status);
		client.debugEntries.setStatus(DebugScreenEntries.GAME_VERSION, status);
	}

	private static String biomeName(Minecraft client, BlockPos pos) {
		Holder<Biome> biome = client.level.getBiome(pos);
		return biome.unwrap().map(QuickInfoDebugEntry::shortenBiomeId, unregistered -> "unknown");
	}

	/** Drops the "minecraft:" namespace for vanilla biomes; keeps it for modded/datapack ones to avoid ambiguity. */
	private static String shortenBiomeId(ResourceKey<Biome> key) {
		Identifier id = key.identifier();
		return id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? id.getPath() : id.toString();
	}
}
