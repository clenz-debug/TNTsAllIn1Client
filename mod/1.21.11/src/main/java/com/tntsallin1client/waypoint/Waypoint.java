package com.tntsallin1client.waypoint;

/**
 * A single user-created waypoint - plain block position plus the dimension it belongs to
 * (waypoints only ever render/count distance while standing in that same dimension, see
 * {@link WaypointRenderer}). Held directly inside {@link com.tntsallin1client.config.ClientConfig}'s
 * list, same "no separate id, screens operate on the live list reference" approach as
 * {@code ClientConfig#pinnedRecipe}.
 */
public class Waypoint {
	public String name = "Waypoint";
	public int x;
	public int y;
	public int z;
	public String dimension = "minecraft:overworld";
	public int color = 0xFFFFFFFF;
	public boolean visible = true;
}
