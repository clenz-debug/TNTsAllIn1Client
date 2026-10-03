package com.tntsallin1client.hud;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

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
	protected boolean isHidden(MinecraftClient client) {
		return client.options.debugEnabled;
	}

	@Override
	public int width(MinecraftClient client) {
		return totalWidth(visibleRows(ClientConfig.get()), client.textRenderer);
	}

	@Override
	public int height(MinecraftClient client) {
		int rows = visibleRows(ClientConfig.get()).size();
		return rows == 0 ? 0 : rows * BOX_HEIGHT + (rows - 1) * GAP;
	}

	@Override
	public int defaultX(MinecraftClient client, int screenWidth, int screenHeight) {
		return screenWidth - EDGE_MARGIN - width(client);
	}

	@Override
	public int defaultY(MinecraftClient client, int screenWidth, int screenHeight) {
		return screenHeight - EDGE_MARGIN - height(client);
	}

	@Override
	protected void draw(MinecraftClient client, ClientConfig config) {
		TextRenderer textRenderer = client.textRenderer;
		List<List<KeystrokeKey>> rows = visibleRows(config);
		int totalWidth = totalWidth(rows, textRenderer);
		// Only the color's RGB is the player's choice - the box stays see-through.
		int activeColor = (config.keystrokesActiveColor & 0xFFFFFF) | ACTIVE_ALPHA;

		int rowY = 0;
		for (List<KeystrokeKey> row : rows) {
			int boxX = (totalWidth - rowWidth(row, textRenderer)) / 2;
			for (KeystrokeKey key : row) {
				int labelWidth = textRenderer.getStringWidth(key.label);
				int boxWidth = labelWidth + BOX_PADDING;
				DrawableHelper.fill(boxX, rowY, boxX + boxWidth, rowY + BOX_HEIGHT, isDown(client.options, key) ? activeColor : BOX_COLOR_IDLE);
				textRenderer.drawWithShadow(key.label, boxX + (boxWidth - labelWidth) / 2, rowY + (BOX_HEIGHT - textRenderer.fontHeight) / 2,
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

	private static int totalWidth(List<List<KeystrokeKey>> rows, TextRenderer textRenderer) {
		int max = 0;
		for (List<KeystrokeKey> row : rows) {
			max = Math.max(max, rowWidth(row, textRenderer));
		}
		return max;
	}

	private static int rowWidth(List<KeystrokeKey> row, TextRenderer textRenderer) {
		int width = 0;
		for (KeystrokeKey key : row) {
			width += textRenderer.getStringWidth(key.label) + BOX_PADDING;
		}
		return width + (row.size() - 1) * GAP;
	}

	private static boolean isDown(GameOptions options, KeystrokeKey key) {
		return binding(options, key).isPressed();
	}

	private static KeyBinding binding(GameOptions options, KeystrokeKey key) {
		switch (key) {
			case W:
				return options.forwardKey;
			case A:
				return options.leftKey;
			case S:
				return options.backKey;
			case D:
				return options.rightKey;
			case SHIFT:
				return options.sneakKey;
			case SPACE:
				return options.jumpKey;
			case LMB:
				return options.attackKey;
			case RMB:
				return options.useKey;
			case SPRINT:
				return options.sprintKey;
			case DROP:
				return options.dropKey;
			default:
				throw new IllegalArgumentException(key.name());
		}
	}
}
