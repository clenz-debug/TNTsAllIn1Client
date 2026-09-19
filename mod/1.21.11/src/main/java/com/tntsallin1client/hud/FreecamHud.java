package com.tntsallin1client.hud;

import com.tntsallin1client.freecam.FreecamHandler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Fixed on-screen reminder while {@link FreecamHandler#isActive()} - not independently
 * configurable/drag-positionable like the mod's other HUD elements, it's tied 1:1 to freecam
 * being active rather than being its own feature. Worth having: while frozen and unable to move,
 * attack or interact, it'd otherwise be easy to forget freecam is even the reason why.
 */
public class FreecamHud implements HudElement {
	private static final int TOP = 4;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (!FreecamHandler.isActive()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		guiGraphics.drawCenteredString(client.font, Component.translatable("gui.tntsallin1client.freecam.hud_active"),
				guiGraphics.guiWidth() / 2, TOP, 0xFFFFFF55);
	}
}
