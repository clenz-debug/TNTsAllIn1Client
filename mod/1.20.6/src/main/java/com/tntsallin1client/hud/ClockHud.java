package com.tntsallin1client.hud;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Always-visible clock - own wishlist item: the real time of day (the computer's own, not the
 * world's), hours and minutes, as "00:00" (24-hour) or with AM/PM (12-hour). Same shared
 * {@link HudLayout} drag/scale/color mechanics as {@link FpsCounterHud}. Defaults to the top-right
 * corner, below where {@link LatencyHud} sits so the two don't overlap by default.
 */
public class ClockHud implements HudElement {
	private static final int DEFAULT_RIGHT_MARGIN = 4;
	private static final int DEFAULT_TOP = 40;
	private static final DateTimeFormatter FORMAT_24_HOUR = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	/** English on purpose - "AM"/"PM" in every game language, not e.g. German's "vorm."/"nachm.". */
	private static final DateTimeFormatter FORMAT_12_HOUR = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

	@Override
	public void render(GuiGraphics guiGraphics, float partialTick) {
		ClientConfig config = ClientConfig.get();
		if (!config.clockHudEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.getDebugOverlay().showDebugScreen()) {
			return;
		}

		String label = buildLabel(config);

		HudLayout layout = config.clockHudLayout;
		float x;
		float y;
		if (layout.customPosition) {
			x = layout.x;
			y = layout.y;
		} else {
			x = defaultX(guiGraphics.guiWidth(), client.font, label);
			y = DEFAULT_TOP;
		}
		FpsCounterHud.drawLabel(guiGraphics, client.font, label, x, y, layout.scale, config.clockTextColor);
	}

	/** The label this HUD would currently show - shared with the HUD editor for accurate drag bounds. */
	public static String buildLabel(ClientConfig config) {
		return LocalTime.now().format(config.clockHud24Hour ? FORMAT_24_HOUR : FORMAT_12_HOUR);
	}

	/** Right-aligned default X for the un-customized position - shared with the HUD editor for accurate drag bounds. */
	public static int defaultX(int guiWidth, Font font, String label) {
		return guiWidth - DEFAULT_RIGHT_MARGIN - font.width(label);
	}

	public static int defaultY() {
		return DEFAULT_TOP;
	}
}
