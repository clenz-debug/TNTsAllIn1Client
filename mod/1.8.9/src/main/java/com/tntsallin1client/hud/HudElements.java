package com.tntsallin1client.hud;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/** Every HUD element of the mod - what the game HUD draws and the HUD editor offers. */
public final class HudElements {
	public static final List<HudElement> ALL = Collections.unmodifiableList(Arrays.<HudElement>asList(
			new CoordinatesHud(),
			new FpsCounterHud(),
			new LatencyHud(),
			new ClockHud()
	));

	private HudElements() {
	}

	/** Called after the game's own HUD, so ours sits on top of it. */
	public static void renderAll(MinecraftClient client) {
		ClientConfig config = ClientConfig.get();
		Window window = new Window(client);
		for (HudElement element : ALL) {
			element.render(client, config, window.getWidth(), window.getHeight());
		}
	}
}
