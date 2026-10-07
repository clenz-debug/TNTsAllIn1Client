package com.tntsallin1client.hud;

import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Always-visible latency (ping) in ms. Sits in the top-right corner below the FPS counter until
 * moved, so the two don't overlap by default.
 */
public final class LatencyHud extends TextHudElement {
	private static final int DEFAULT_TOP = 16;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.latency_hud";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.latencyHudEnabled;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.latencyHudLayout;
	}

	@Override
	protected List<String> lines(Minecraft client, ClientConfig config) {
		return Collections.singletonList(latencyMs(client) + " ms");
	}

	/**
	 * The local player's own ping from the tab list - the same value the tab list shows, just always
	 * visible. 0 while there is no connection or the entry hasn't arrived yet.
	 */
	private static int latencyMs(Minecraft client) {
		ClientPacketListener networkHandler = client.getConnection();
		if (networkHandler == null || client.player == null) {
			return 0;
		}
		PlayerInfo entry = networkHandler.getPlayerInfo(client.player.getUUID());
		return entry != null ? entry.getLatency() : 0;
	}

	@Override
	protected int color(ClientConfig config) {
		return config.latencyTextColor;
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return rightAlignedX(client, screenWidth);
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return DEFAULT_TOP;
	}
}
