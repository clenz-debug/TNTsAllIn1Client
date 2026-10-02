package com.tntsallin1client.hud;

import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Always-visible FPS counter, so it's not necessary to open F3 just to see the frame rate. Sits in
 * the top-right corner until moved.
 */
public final class FpsCounterHud extends TextHudElement {
	private static final int DEFAULT_TOP = 4;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.fps_counter";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.fpsCounterEnabled;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.fpsCounterHudLayout;
	}

	@Override
	protected List<String> lines(MinecraftClient client, ClientConfig config) {
		return Collections.singletonList(MinecraftClient.getCurrentFps() + " FPS");
	}

	@Override
	protected int color(ClientConfig config) {
		return config.fpsCounterTextColor;
	}

	@Override
	public int defaultX(MinecraftClient client, int screenWidth, int screenHeight) {
		return rightAlignedX(client, screenWidth);
	}

	@Override
	public int defaultY(MinecraftClient client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}
}
