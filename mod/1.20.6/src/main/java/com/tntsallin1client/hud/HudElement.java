package com.tntsallin1client.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One of our HUD parts. Fabric API for 1.20.6 has no HUD element registry yet (that came with 1.21.6),
 * so this stands in for its {@code HudElement} - see {@link HudElements}.
 */
@FunctionalInterface
public interface HudElement {
	void render(GuiGraphics guiGraphics, float partialTick);
}
