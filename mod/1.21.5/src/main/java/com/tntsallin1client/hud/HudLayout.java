package com.tntsallin1client.hud;

/**
 * Phase 5 (HUD editor): position/scale override for one HUD element. While
 * {@code customPosition} is false, the element uses its own built-in default
 * placement and {@code x}/{@code y} are unused - the editor only starts writing
 * real values once the user actually drags or resizes that element, seeded from
 * wherever it was currently rendering.
 */
public class HudLayout {
	public boolean customPosition = false;
	public float x = 0f;
	public float y = 0f;
	public float scale = 1.0f;
}
