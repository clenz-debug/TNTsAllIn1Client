package com.tntsallin1client;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.inventory.ContainerClickPacing;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.menu.WaypointMenuIntegration;
import com.tntsallin1client.screenshot.ScreenshotWatcher;
import com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of the 1.14.4 port. The features of the other versions are brought over one by one;
 * what is in already is registered here.
 */
public class TNTsAllIn1ClientMod implements ClientModInitializer {
	public static final String MOD_ID = "tntsallin1client";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ModKeyBindings.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			FreecamHandler.tick(client);
			WaypointMenuIntegration.tick(client);
			SpawnOverlayRenderer.tick(client);
			ContainerClickPacing.tick(client);
			ScreenshotWatcher.tick(client);
			while (ModKeyBindings.OPEN_MENU.consumeClick()) {
				// Only from gameplay - with a screen open the key belongs to that screen.
				if (client.screen == null) {
					client.setScreen(ClientMenus.create(null));
				}
			}
		});

		LOGGER.info("[{}] Client mod initialized.", MOD_ID);
	}
}
