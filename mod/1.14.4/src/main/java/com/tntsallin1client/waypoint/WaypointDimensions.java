package com.tntsallin1client.waypoint;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * The dimension a waypoint is stored with, and its name on screen. The game names its dimensions
 * itself here ("minecraft:overworld") - the same names the other versions of the mod store.
 */
public final class WaypointDimensions {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";
	public static final String END = "minecraft:the_end";

	private WaypointDimensions() {
	}

	/** What a waypoint set in this world gets as its {@link Waypoint#dimension}. */
	public static String key(Level level) {
		ResourceLocation name = DimensionType.getName(level.dimension.getType());
		return name != null ? name.toString() : OVERWORLD;
	}

	public static String label(String dimensionKey) {
		if (OVERWORLD.equals(dimensionKey)) {
			return I18n.get("gui.tntsallin1client.waypoint_list.dimension.overworld");
		}
		if (NETHER.equals(dimensionKey)) {
			return I18n.get("gui.tntsallin1client.waypoint_list.dimension.the_nether");
		}
		if (END.equals(dimensionKey)) {
			return I18n.get("gui.tntsallin1client.waypoint_list.dimension.the_end");
		}
		return dimensionKey;
	}
}
