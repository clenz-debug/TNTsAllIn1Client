package com.tntsallin1client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Our HUD parts, drawn in the order they were added, on top of the game's own HUD - what
 * {@code HudElementRegistry.addLast} does in the newer versions. The game's HUD layers each check
 * F1 themselves in 1.21.1, and Fabric's callback runs after them regardless, so that check is here.
 */
public final class HudElements {
	private static final List<HudElement> ELEMENTS = new ArrayList<>();
	private static boolean registered;

	private HudElements() {
	}

	public static void add(HudElement element) {
		ELEMENTS.add(element);
		if (!registered) {
			registered = true;
			HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
				if (Minecraft.getInstance().options.hideGui) {
					return;
				}
				for (HudElement each : ELEMENTS) {
					each.render(guiGraphics, deltaTracker);
				}
			});
		}
	}
}
