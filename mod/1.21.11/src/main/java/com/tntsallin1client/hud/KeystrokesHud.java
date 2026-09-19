package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

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

	private record KeyEntry(KeystrokeKey key, boolean down) {
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

		List<List<KeyEntry>> rows = buildRows(client, config);
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
				String label = entry.key().label;
				int boxWidth = boxWidth(label, font);
				guiGraphics.fill(boxX, rowY, boxX + boxWidth, rowY + BOX_HEIGHT, entry.down() ? activeColor : BOX_COLOR_IDLE);
				guiGraphics.drawCenteredString(font, label, boxX + boxWidth / 2, rowY + (BOX_HEIGHT - font.lineHeight) / 2, config.keystrokesTextColor);
				boxX += boxWidth + GAP;
			}
			rowY += BOX_HEIGHT + GAP;
		}

		guiGraphics.pose().popMatrix();
	}

	/** Un-customized total box-grid width - shared with the HUD editor for accurate drag bounds. */
	public static int computeTotalWidth(Minecraft client) {
		return totalWidth(buildRows(client, ClientConfig.get()), client.font);
	}

	/** Un-customized total box-grid height - shared with the HUD editor for accurate drag bounds. */
	public static int computeTotalHeight(Minecraft client) {
		return totalHeight(buildRows(client, ClientConfig.get()));
	}

	private static int totalWidth(List<List<KeyEntry>> rows, Font font) {
		int max = 0;
		for (List<KeyEntry> row : rows) {
			max = Math.max(max, rowWidth(row, font));
		}
		return max;
	}

	private static int totalHeight(List<List<KeyEntry>> rows) {
		return rows.isEmpty() ? 0 : rows.size() * BOX_HEIGHT + (rows.size() - 1) * GAP;
	}

	private static int rowWidth(List<KeyEntry> row, Font font) {
		int width = 0;
		for (KeyEntry entry : row) {
			width += boxWidth(entry.key().label, font);
		}
		return width + (row.size() - 1) * GAP;
	}

	private static int boxWidth(String label, Font font) {
		return font.width(label) + BOX_PADDING;
	}

	/** Rows with any individually-disabled keys (see {@link KeystrokeKey}) filtered out; empty rows are dropped entirely rather than left as gaps. */
	private static List<List<KeyEntry>> buildRows(Minecraft client, ClientConfig config) {
		List<List<KeyEntry>> rows = new ArrayList<>();
		for (List<KeystrokeKey> row : KeystrokeKey.ROWS) {
			List<KeyEntry> visible = new ArrayList<>();
			for (KeystrokeKey key : row) {
				if (config.keystrokesKeyEnabled.getOrDefault(key.name(), true)) {
					visible.add(new KeyEntry(key, isDown(client, key)));
				}
			}
			if (!visible.isEmpty()) {
				rows.add(visible);
			}
		}
		return rows;
	}

	private static boolean isDown(Minecraft client, KeystrokeKey key) {
		var options = client.options;
		return switch (key) {
			case W -> options.keyUp.isDown();
			case A -> options.keyLeft.isDown();
			case S -> options.keyDown.isDown();
			case D -> options.keyRight.isDown();
			case SHIFT -> options.keyShift.isDown();
			case SPACE -> options.keyJump.isDown();
			case LMB -> options.keyAttack.isDown();
			case RMB -> options.keyUse.isDown();
			case SPRINT -> options.keySprint.isDown();
			case DROP -> options.keyDrop.isDown();
		};
	}
}
