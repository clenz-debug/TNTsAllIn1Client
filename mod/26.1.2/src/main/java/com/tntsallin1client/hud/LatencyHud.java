package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Always-visible latency (ping) counter in ms - own wishlist item, "wie die
 * FPS-Anzeige, aber Latenz in ms statt FPS" (same shared {@link HudLayout}
 * drag/scale/color mechanics as {@link FpsCounterHud}, just a different
 * number source). Defaults to the top-right corner, below where
 * {@link FpsCounterHud} sits so the two don't overlap by default - same
 * "each one shifted further down" convention {@link FpsCounterHud}'s own doc
 * comment describes relative to {@link ItemCounterHud}.
 */
public class LatencyHud implements HudElement {
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_TOP = 28;

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		ClientConfig config = ClientConfig.get();
		if (!config.latencyHudEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		String label = buildLabel(client);

		HudLayout layout = config.latencyHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = defaultX(guiGraphics.guiWidth(), client.font, label);
			y = DEFAULT_TOP;
		}
		FpsCounterHud.drawLabel(guiGraphics, client.font, label, x, y, layout.scale, config.latencyTextColor);
	}

	/**
	 * The label this HUD would currently show - shared with the HUD editor for accurate drag bounds.
	 * 0 ms while no connection/player-info is available yet (e.g. the brief moment before joining
	 * fully resolves) rather than leaving the label blank.
	 */
	public static String buildLabel(Minecraft client) {
		return latencyMs(client) + " ms";
	}

	/** The local player's own ping, straight off the tab list's own PlayerInfo - same value vanilla's tab list/F3 show, just always visible. */
	private static int latencyMs(Minecraft client) {
		ClientPacketListener connection = client.getConnection();
		if (connection == null || client.player == null) {
			return 0;
		}
		PlayerInfo info = connection.getPlayerInfo(client.player.getUUID());
		return info != null ? info.getLatency() : 0;
	}

	/** Right-aligned default X for the un-customized position - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX(int guiWidth, Font font, String label) {
		return guiWidth - DEFAULT_RIGHT_MARGIN - font.width(label);
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}
}
