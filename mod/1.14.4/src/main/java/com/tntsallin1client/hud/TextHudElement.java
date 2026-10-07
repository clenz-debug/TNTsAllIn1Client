package com.tntsallin1client.hud;

import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;

/**
 * A HUD element that is one or more lines of text in one color - which covers most of them. A
 * subclass only says what the lines are, which color they have and where they sit by default.
 */
public abstract class TextHudElement extends HudElement {
	protected static final int EDGE_MARGIN = 4;

	/** The lines to show right now, top to bottom. Empty if there is nothing to show. */
	protected abstract List<String> lines(Minecraft client, ClientConfig config);

	protected abstract int color(ClientConfig config);

	/** F3 shows all of this itself and uses the same corners. */
	@Override
	protected boolean isHidden(Minecraft client) {
		return client.options.renderDebug;
	}

	@Override
	public int width(Minecraft client) {
		int width = 0;
		for (String line : lines(client, ClientConfig.get())) {
			width = Math.max(width, client.font.width(line));
		}
		return width;
	}

	@Override
	public int height(Minecraft client) {
		return lines(client, ClientConfig.get()).size() * client.font.lineHeight;
	}

	/** Default X for an element that sits at the right edge of the screen. */
	protected int rightAlignedX(Minecraft client, int screenWidth) {
		return screenWidth - EDGE_MARGIN - width(client);
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		int lineY = 0;
		for (String line : lines(client, config)) {
			client.font.drawShadow(line, 0, lineY, color(config));
			lineY += client.font.lineHeight;
		}
	}
}
