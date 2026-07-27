package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Phase 5m: small grid of key/mouse boxes that light up while held - the
 * classic speedrun/PvP-montage "keystrokes" overlay. Goes a bit past the
 * "classic" WASD/LMB/RMB/Shift/Space set the idea itself named, adding the
 * dedicated sprint key and drop (Q) as two extra rows. Reads vanilla's own
 * {@code Options} KeyMappings directly (no custom keybinds to rebind here -
 * whatever the player already has bound for movement/attack/etc. lights up),
 * so this only makes sense during normal gameplay, same as the other HUD
 * elements. Defaults to the bottom-right corner, the one HUD corner not
 * already used by another Phase 5 feature. Box widths are sized to each
 * label's text width rather than a fixed square, since "Shift"/"Sprint" don't
 * fit in the same box as "W".
 */
public class KeystrokesHud implements HudElement {
	private static final int BOX_HEIGHT = 14;
	private static final int BOX_PADDING = 6;
	private static final int GAP = 2;
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_BOTTOM_MARGIN = 4;
	private static final int BOX_COLOR_IDLE = 0x80333333;
	private static final int ACTIVE_ALPHA = 0xC0000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private record KeyEntry(String label, BooleanSupplier isDown) {
	}

	@Override
	public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.keystrokesEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		List<List<KeyEntry>> rows = buildRows(client);
		Font font = client.font;
		int totalWidth = totalWidth(rows, font);
		int totalHeight = totalHeight(rows);

		HudLayout layout = config.keystrokesHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = guiGraphics.guiWidth() - DEFAULT_RIGHT_MARGIN - totalWidth;
			y = guiGraphics.guiHeight() - DEFAULT_BOTTOM_MARGIN - totalHeight;
		}

		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(layout.scale);

		int activeColor = (config.keystrokesActiveColor & 0xFFFFFF) | ACTIVE_ALPHA;

		int rowY = 0;
		for (List<KeyEntry> row : rows) {
			int rowWidth = rowWidth(row, font);
			int boxX = (totalWidth - rowWidth) / 2;
			for (KeyEntry entry : row) {
				int boxWidth = boxWidth(entry.label(), font);
				boolean down = entry.isDown().getAsBoolean();
				guiGraphics.fill(boxX, rowY, boxX + boxWidth, rowY + BOX_HEIGHT, down ? activeColor : BOX_COLOR_IDLE);
				guiGraphics.drawCenteredString(font, entry.label(), boxX + boxWidth / 2, rowY + (BOX_HEIGHT - font.lineHeight) / 2, TEXT_COLOR);
				boxX += boxWidth + GAP;
			}
			rowY += BOX_HEIGHT + GAP;
		}

		guiGraphics.pose().popMatrix();
	}

	/** Un-customized total box-grid width - shared with the HUD editor for accurate drag bounds. */
	public static int computeTotalWidth(Minecraft client) {
		return totalWidth(buildRows(client), client.font);
	}

	/** Un-customized total box-grid height - shared with the HUD editor for accurate drag bounds. */
	public static int computeTotalHeight(Minecraft client) {
		return totalHeight(buildRows(client));
	}

	private static int totalWidth(List<List<KeyEntry>> rows, Font font) {
		int max = 0;
		for (List<KeyEntry> row : rows) {
			max = Math.max(max, rowWidth(row, font));
		}
		return max;
	}

	private static int totalHeight(List<List<KeyEntry>> rows) {
		return rows.size() * BOX_HEIGHT + (rows.size() - 1) * GAP;
	}

	private static int rowWidth(List<KeyEntry> row, Font font) {
		int width = 0;
		for (KeyEntry entry : row) {
			width += boxWidth(entry.label(), font);
		}
		return width + (row.size() - 1) * GAP;
	}

	private static int boxWidth(String label, Font font) {
		return font.width(label) + BOX_PADDING;
	}

	private static List<List<KeyEntry>> buildRows(Minecraft client) {
		var options = client.options;
		return List.of(
				List.of(new KeyEntry("W", options.keyUp::isDown)),
				List.of(
						new KeyEntry("A", options.keyLeft::isDown),
						new KeyEntry("S", options.keyDown::isDown),
						new KeyEntry("D", options.keyRight::isDown)),
				List.of(
						new KeyEntry("Shift", options.keyShift::isDown),
						new KeyEntry("Space", options.keyJump::isDown)),
				List.of(
						new KeyEntry("LMB", options.keyAttack::isDown),
						new KeyEntry("RMB", options.keyUse::isDown)),
				List.of(
						new KeyEntry("Sprint", options.keySprint::isDown),
						new KeyEntry("Drop", options.keyDrop::isDown)));
	}
}
