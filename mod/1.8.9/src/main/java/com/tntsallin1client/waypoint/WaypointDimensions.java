package com.tntsallin1client.waypoint;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.world.World;

/**
 * The dimension a waypoint is stored with, and its name on screen. This version numbers its
 * dimensions; the three the game has are stored under the names the Fabric versions of the mod
 * store them with. Anything else (a server's own dimension) keeps its number.
 */
public final class WaypointDimensions {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";
	public static final String END = "minecraft:the_end";

	private static final int OVERWORLD_ID = 0;
	private static final int NETHER_ID = -1;
	private static final int END_ID = 1;

	private WaypointDimensions() {
	}

	/** What a waypoint set in this world gets as its {@link Waypoint#dimension}. */
	public static String key(World world) {
		int id = world.dimension.getType();
		switch (id) {
			case OVERWORLD_ID:
				return OVERWORLD;
			case NETHER_ID:
				return NETHER;
			case END_ID:
				return END;
			default:
				return "dimension:" + id;
		}
	}

	public static String label(String dimensionKey) {
		if (OVERWORLD.equals(dimensionKey)) {
			return I18n.translate("gui.tntsallin1client.waypoint_list.dimension.overworld");
		}
		if (NETHER.equals(dimensionKey)) {
			return I18n.translate("gui.tntsallin1client.waypoint_list.dimension.the_nether");
		}
		if (END.equals(dimensionKey)) {
			return I18n.translate("gui.tntsallin1client.waypoint_list.dimension.the_end");
		}
		return dimensionKey;
	}
}
