package com.tntsallin1client.debug;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;

/**
 * Phase 5d, second iteration: CPU/GPU/Java/Minecraft-version info moved out of
 * the main F3 screen into its own page, toggled with F3+S (unused by vanilla in
 * this version - see the keyDebugXxx defaults in Options.java, none bind "S").
 * Reuses vanilla's own {@code DebugScreenEntry} objects for the actual data
 * gathering (real hardware queries; nothing here should be reimplemented) via
 * {@link LineCollectingDisplayer}, rather than duplicating that logic.
 */
public class SystemInfoOverlay implements HudElement {
	// Not persisted - resets to hidden on every launch, matching vanilla F3's own behavior.
	public static boolean visible = false;

	private static final Identifier[] ENTRIES = {DebugScreenEntries.SYSTEM_SPECS, DebugScreenEntries.GAME_VERSION};
	private static final int BACKGROUND_COLOR = 0x90000000;
	private static final int PADDING = 4;

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		if (!visible) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LineCollectingDisplayer displayer = new LineCollectingDisplayer();
		for (Identifier id : ENTRIES) {
			DebugScreenEntry entry = DebugScreenEntries.getEntry(id);
			if (entry != null) {
				entry.display(displayer, null, null, null);
			}
		}
		if (displayer.lines.isEmpty()) {
			return;
		}

		int width = 0;
		for (String line : displayer.lines) {
			width = Math.max(width, client.font.width(line));
		}
		int height = displayer.lines.size() * client.font.lineHeight;
		int left = (guiGraphics.guiWidth() - width) / 2;
		int top = (guiGraphics.guiHeight() - height) / 2;

		guiGraphics.fill(left - PADDING, top - PADDING, left + width + PADDING, top + height + PADDING, BACKGROUND_COLOR);
		int textColor = ClientConfig.get().systemInfoTextColor;
		int y = top;
		for (String line : displayer.lines) {
			guiGraphics.drawString(client.font, line, left, y, textColor);
			y += client.font.lineHeight;
		}
	}
}
