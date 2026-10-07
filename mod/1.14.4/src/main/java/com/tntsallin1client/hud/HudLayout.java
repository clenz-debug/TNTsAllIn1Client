package com.tntsallin1client.hud;

/**
 * Position/scale override for one HUD element. While {@code customPosition} is false, the element
 * uses its own built-in default placement and {@code x}/{@code y} are unused - the HUD editor only
 * starts writing real values once the player actually drags or resizes that element, seeded from
 * wherever it was currently rendering.
 */
public class HudLayout {
	public boolean customPosition = false;
	public float x = 0f;
	public float y = 0f;
	public float scale = 1.0f;

	public void reset() {
		this.customPosition = false;
		this.x = 0f;
		this.y = 0f;
		this.scale = 1.0f;
	}
}
