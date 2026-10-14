package com.tntsallin1client.waypoint;

import com.tntsallin1client.config.ClientConfig;

/**
 * A single user-created waypoint - plain block position plus the dimension it belongs to
 * (waypoints only ever render/count distance while standing in that same dimension, see
 * {@link WaypointRenderer}). Held directly inside {@link com.tntsallin1client.config.ClientConfig}'s
 * list, same "no separate id, screens operate on the live list reference" approach as
 * {@code ClientConfig#pinnedRecipe}.
 *
 * <p>Beam, marker, distance and fade are per waypoint; the matching {@code ClientConfig#waypointShow*}/
 * {@code #waypointFadeNearby} settings are only the defaults a new waypoint starts with
 * (see {@link #applyDisplayDefaults}).
 */
public class Waypoint {
	public String name = "Waypoint";
	public int x;
	public int y;
	public int z;
	public String dimension = "minecraft:overworld";
	public int color = 0xFFFFFFFF;
	public boolean visible = true;
	public boolean showBeam = true;
	public boolean showMarker = true;
	public boolean showDistance = true;
	public boolean fadeNearby = false;

	public void applyDisplayDefaults(ClientConfig config) {
		this.showBeam = config.waypointShowBeam;
		this.showMarker = config.waypointShowMarker;
		this.showDistance = config.waypointShowDistance;
		this.fadeNearby = config.waypointFadeNearby;
	}
}
