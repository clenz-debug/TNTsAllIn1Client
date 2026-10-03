package com.tntsallin1client.waypoint;

import com.tntsallin1client.config.ClientConfig;

/**
 * A waypoint the player set: a block position plus the dimension it belongs to - a waypoint is only
 * drawn, and its distance only counted, while standing in that dimension. Kept in the config's list
 * as it is; the screens work on the object in that list, not on a copy.
 *
 * <p>Beam, marker, distance and fading are set per waypoint; the config's settings of the same names
 * are only what a new waypoint starts with (see {@link #applyDisplayDefaults}).
 */
public class Waypoint {
	public String name = "Waypoint";
	public int x;
	public int y;
	public int z;
	/** See {@link WaypointDimensions#key}. */
	public String dimension = WaypointDimensions.OVERWORLD;
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
