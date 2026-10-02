package com.tntsallin1client.debug;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudLayout;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Phase 5d, second iteration: CPU/GPU/Java/Minecraft-version info moved out of
 * the main F3 screen into its own page, toggled with F3+S (unused by vanilla in
 * this version - see the keyDebugXxx defaults in Options.java, none bind "S").
 * Reuses vanilla's own {@code DebugScreenEntry} objects for the actual data
 * gathering (real hardware queries; nothing here should be reimplemented) via
 * {@link LineCollectingDisplayer}, rather than duplicating that logic.
 *
 * <p>Movable/resizable like every other HUD element (see the HUD editor) even though visibility
 * itself is the transient F3+S toggle, not a persisted "enabled" setting - positioning still needs
 * to persist independently of that, same {@link ClientConfig#systemInfoHudLayout} either way.
 */
public class SystemInfoOverlay implements HudElement {
	// Not persisted - resets to hidden on every launch, matching vanilla F3's own behavior.
	public static boolean visible = false;

	private static final Identifier[] ENTRIES = {DebugScreenEntries.SYSTEM_SPECS, DebugScreenEntries.GAME_VERSION};
	private static final int BACKGROUND_COLOR = 0x90000000;
	private static final int PADDING = 4;

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		if (!visible) {
			return;
		}

		List<String> lines = buildLines();
		if (lines.isEmpty()) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		int width = contentWidth(font, lines);
		int height = contentHeight(font, lines);

		ClientConfig config = ClientConfig.get();
		HudLayout layout = config.systemInfoHudLayout;
		float x = layout.customPosition ? layout.x : defaultX(guiGraphics.guiWidth(), width);
		float y = layout.customPosition ? layout.y : defaultY(guiGraphics.guiHeight(), height);

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(layout.scale);

		guiGraphics.fill(-PADDING, -PADDING, width + PADDING, height + PADDING, BACKGROUND_COLOR);
		int textColor = config.systemInfoTextColor;
		int lineY = 0;
		for (String line : lines) {
			guiGraphics.text(font, line, 0, lineY, textColor);
			lineY += font.lineHeight;
		}

		guiGraphics.pose().popMatrix();
	}

	/** Lines this overlay would currently show, in render order. Real hardware/version queries, no
	 * world or player needed (verified: {@code DebugScreenEntry#display} is always called with null
	 * level/chunk context here, same as the live render path) - shared with the HUD editor so it can
	 * be positioned even from the title screen. */
	public static List<String> buildLines() {
		LineCollectingDisplayer displayer = new LineCollectingDisplayer();
		for (Identifier id : ENTRIES) {
			DebugScreenEntry entry = DebugScreenEntries.getEntry(id);
			if (entry != null) {
				entry.display(displayer, null, null, null);
			}
		}
		return displayer.lines;
	}

	/** Unscaled pixel width of the whole box - shared with the HUD editor's drag bounds. */
	public static int contentWidth(Font font, List<String> lines) {
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		return width;
	}

	/** Unscaled pixel height of the whole box - shared with the HUD editor's drag bounds. */
	public static int contentHeight(Font font, List<String> lines) {
		return lines.size() * font.lineHeight;
	}

	public static float defaultX(int guiWidth, int width) {
		return (guiWidth - width) / 2f;
	}

	public static float defaultY(int guiHeight, int height) {
		return (guiHeight - height) / 2f;
	}
}
