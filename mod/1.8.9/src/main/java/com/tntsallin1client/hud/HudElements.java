package com.tntsallin1client.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/** Every HUD element of the mod - what the game HUD draws and the HUD editor offers. */
public final class HudElements {
	public static final List<HudElement> ALL = Collections.unmodifiableList(all());

	private HudElements() {
	}

	private static List<HudElement> all() {
		List<HudElement> elements = new ArrayList<HudElement>();
		elements.add(new CoordinatesHud());
		elements.add(new ItemCounterHud());
		elements.add(new FpsCounterHud());
		elements.add(new LatencyHud());
		elements.add(new ClockHud());
		elements.add(new KeystrokesHud());
		// The Armor & Tool Status display is either the one bundled element or one per slot.
		elements.add(new ArmorStatusBundledHud());
		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
			elements.add(new ArmorStatusSlotHud(slot));
		}
		return elements;
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
