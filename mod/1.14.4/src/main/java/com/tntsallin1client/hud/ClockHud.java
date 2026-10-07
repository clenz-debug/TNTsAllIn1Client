package com.tntsallin1client.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Always-visible clock: the real time of day (the computer's own, not the world's), hours and
 * minutes, as "00:00" (24-hour) or with AM/PM (12-hour). Sits in the top-right corner below the
 * latency display until moved.
 */
public final class ClockHud extends TextHudElement {
	private static final int DEFAULT_TOP = 28;
	private static final DateTimeFormatter FORMAT_24_HOUR = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	/** English on purpose - "AM"/"PM" in every game language, not e.g. German's "vorm."/"nachm.". */
	private static final DateTimeFormatter FORMAT_12_HOUR = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.clock_hud";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return config.clockHudEnabled;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.clockHudLayout;
	}

	@Override
	protected List<String> lines(Minecraft client, ClientConfig config) {
		return Collections.singletonList(LocalTime.now().format(config.clockHud24Hour ? FORMAT_24_HOUR : FORMAT_12_HOUR));
	}

	@Override
	protected int color(ClientConfig config) {
		return config.clockTextColor;
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
