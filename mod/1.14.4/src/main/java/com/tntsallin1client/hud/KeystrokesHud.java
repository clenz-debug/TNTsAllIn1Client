package com.tntsallin1client.hud;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;

/**
 * Small grid of key/mouse boxes that light up while held - the classic "keystrokes" overlay, plus
 * the sprint and drop keys as an extra row. Reads the game's own key bindings, so whatever the
 * player has bound for movement/attack/etc. lights up. Box widths follow each label's text width
 * rather than a fixed square, since "Shift"/"Sprint" don't fit in the same box as "W". Sits in the
 * bottom-right corner until moved.
 */
public final class KeystrokesHud extends HudElement {
	private static final int BOX_HEIGHT = 14;
	private static final int BOX_PADDING = 6;
	private static final int GAP = 2;
	private static final int EDGE_MARGIN = 4;
	private static final int BOX_COLOR_IDLE = 0x80333333;
	private static final int ACTIVE_ALPHA = 0xC0000000;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.keystrokes";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.keystrokesEnabled;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.keystrokesHudLayout;
	}

	/** F3 covers the screen with its own text. */
	@Override
	protected boolean isHidden(Minecraft client) {
		return client.options.renderDebug;
	}

	@Override
	public int width(Minecraft client) {
		return totalWidth(visibleRows(ClientConfig.get()), client.font);
	}

	@Override
	public int height(Minecraft client) {
		int rows = visibleRows(ClientConfig.get()).size();
		return rows == 0 ? 0 : rows * BOX_HEIGHT + (rows - 1) * GAP;
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return screenWidth - EDGE_MARGIN - width(client);
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return screenHeight - EDGE_MARGIN - height(client);
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		Font font = client.font;
		List<List<KeystrokeKey>> rows = visibleRows(config);
		int totalWidth = totalWidth(rows, font);
		// Only the color's RGB is the player's choice - the box stays see-through.
		int activeColor = (config.keystrokesActiveColor & 0xFFFFFF) | ACTIVE_ALPHA;

		int rowY = 0;
		for (List<KeystrokeKey> row : rows) {
			int boxX = (totalWidth - rowWidth(row, font)) / 2;
			for (KeystrokeKey key : row) {
				int labelWidth = font.width(key.label);
				int boxWidth = labelWidth + BOX_PADDING;
				GuiComponent.fill(boxX, rowY, boxX + boxWidth, rowY + BOX_HEIGHT, isDown(client.options, key) ? activeColor : BOX_COLOR_IDLE);
				font.drawShadow(key.label, boxX + (boxWidth - labelWidth) / 2, rowY + (BOX_HEIGHT - font.lineHeight) / 2,
						config.keystrokesTextColor);
				boxX += boxWidth + GAP;
			}
			rowY += BOX_HEIGHT + GAP;
		}
	}

	/** The rows without the keys the player switched off; a row with no key left is dropped rather than left as a gap. */
	private static List<List<KeystrokeKey>> visibleRows(ClientConfig config) {
		List<List<KeystrokeKey>> rows = new ArrayList<List<KeystrokeKey>>();
		for (List<KeystrokeKey> row : KeystrokeKey.ROWS) {
			List<KeystrokeKey> visible = new ArrayList<KeystrokeKey>();
			for (KeystrokeKey key : row) {
				if (key.isShown(config)) {
					visible.add(key);
				}
			}
			if (!visible.isEmpty()) {
				rows.add(visible);
			}
		}
		return rows;
	}

	private static int totalWidth(List<List<KeystrokeKey>> rows, Font font) {
		int max = 0;
		for (List<KeystrokeKey> row : rows) {
			max = Math.max(max, rowWidth(row, font));
		}
		return max;
	}

	private static int rowWidth(List<KeystrokeKey> row, Font font) {
		int width = 0;
		for (KeystrokeKey key : row) {
			width += font.width(key.label) + BOX_PADDING;
		}
		return width + (row.size() - 1) * GAP;
	}

	private static boolean isDown(Options options, KeystrokeKey key) {
		return binding(options, key).isDown();
	}

	private static KeyMapping binding(Options options, KeystrokeKey key) {
		switch (key) {
			case W:
				return options.keyUp;
			case A:
				return options.keyLeft;
			case S:
				return options.keyDown;
			case D:
				return options.keyRight;
			case SHIFT:
				return options.keySneak;
			case SPACE:
				return options.keyJump;
			case LMB:
				return options.keyAttack;
			case RMB:
				return options.keyUse;
			case SPRINT:
				return options.keySprint;
			case DROP:
				return options.keyDrop;
			default:
				throw new IllegalArgumentException(key.name());
		}
	}
}
