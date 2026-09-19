package com.tntsallin1client.waypoint;

import net.minecraft.network.chat.Component;

/**
 * Short display label for a waypoint's stored dimension key (see {@link Waypoint#dimension}).
 * Only the three vanilla dimensions get a translated label - anything else (a modded dimension)
 * falls back to the raw key itself rather than guessing a name for it.
 */
public final class WaypointDimensions {
	private WaypointDimensions() {
	}

	public static Component label(String dimensionKey) {
		return switch (dimensionKey) {
			case "minecraft:overworld" -> Component.translatable("gui.tntsallin1client.waypoint_list.dimension.overworld");
			case "minecraft:the_nether" -> Component.translatable("gui.tntsallin1client.waypoint_list.dimension.the_nether");
			case "minecraft:the_end" -> Component.translatable("gui.tntsallin1client.waypoint_list.dimension.the_end");
			default -> Component.literal(dimensionKey);
		};
	}
}
