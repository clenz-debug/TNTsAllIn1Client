package com.tntsallin1client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tntsallin1client.debug.QuickInfoDebugEntry;
import com.tntsallin1client.debug.SystemInfoOverlay;
import com.tntsallin1client.fullbright.FullbrightHandler;
import com.tntsallin1client.hud.CoordinatesHud;
import com.tntsallin1client.hud.FpsCounterHud;
import com.tntsallin1client.hud.KeystrokesHud;
import com.tntsallin1client.hud.LightLevelHud;
import com.tntsallin1client.hud.MaterialCounterHud;
import com.tntsallin1client.inventory.QuickSortUi;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.menu.PauseMenuIntegration;
import com.tntsallin1client.screenshot.ScreenshotWatcher;
import com.tntsallin1client.tooltip.ShulkerContentsTooltip;
import com.tntsallin1client.zoom.ZoomHandler;

public class TNTsAllIn1ClientMod implements ClientModInitializer {
	public static final String MOD_ID = "tntsallin1client";

	// This logger is used to write text to the console and the log file.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("[{}] Hello Fabric world!", MOD_ID);

		// Phase 1 "done" check: a log line and a chat message when a world is joined.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			LOGGER.info("[{}] Joined a world.", MOD_ID);
			client.gui.getChat().addMessage(Component.nullToEmpty("TNT's All-In-1 Client loaded."));
		});

		// Phase 5a: coordinates + compass HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "coordinates_hud"), new CoordinatesHud());

		// Phase 5b: material counter HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "material_counter_hud"), new MaterialCounterHud());

		// Phase 5g: FPS counter HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "fps_counter_hud"), new FpsCounterHud());

		// Phase 5j: light-level HUD + fullbright toggle.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "light_level_hud"), new LightLevelHud());
		ClientTickEvents.END_CLIENT_TICK.register(FullbrightHandler::tick);

		// Phase 5m: keystrokes overlay.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "keystrokes_hud"), new KeystrokesHud());

		// Phase 5n: screenshot Open/Copy toast.
		ClientTickEvents.END_CLIENT_TICK.register(ScreenshotWatcher::tick);

		// Phase 5c: inventory quick-sort (button + keybind).
		ModKeyBindings.register();
		QuickSortUi.register();

		// Phase 5e: ingame mod menu (keybind + pause menu button).
		PauseMenuIntegration.register();

		// Phase 5d: extra "Quick Info" block on the F3 debug screen, plus a
		// separate F3+S page for CPU/GPU/version (moved out of the main screen).
		Identifier quickInfoId = Identifier.fromNamespaceAndPath(MOD_ID, "quick_info");
		DebugScreenEntries.register(quickInfoId, new QuickInfoDebugEntry());
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "system_info_overlay"), new SystemInfoOverlay());
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			client.debugEntries.setStatus(quickInfoId, DebugScreenEntryStatus.IN_OVERLAY);
			QuickInfoDebugEntry.applyVanillaEntryVisibility(client);
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.options.keyDebugModifier.isDown() && ModKeyBindings.SYSTEM_INFO.consumeClick()) {
				SystemInfoOverlay.visible = !SystemInfoOverlay.visible;
			}
		});

		// Phase 5h: hold-to-zoom.
		ClientTickEvents.END_CLIENT_TICK.register(ZoomHandler::tick);

		// Phase 5k: hold-to-preview shulker box contents.
		ShulkerContentsTooltip.register();
	}
}
