package com.tntsallin1client.screenshot;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Notices new screenshots by looking into the `screenshots` folder every few ticks, as the Fabric
 * versions do - it does not matter how the screenshot was taken, only that a new file showed up.
 * Each one gets a chat message with links to open and to copy it ({@link ScreenshotChatLink}).
 */
public final class ScreenshotWatcher {
	private static final int POLL_INTERVAL_TICKS = 10;

	private static boolean initialized;
	private static final Set<String> KNOWN_FILE_NAMES = new HashSet<String>();
	private static int ticksSincePoll;

	private ScreenshotWatcher() {
	}

	public static void tick(MinecraftClient client) {
		if (!ClientConfig.get().screenshotToastEnabled) {
			return;
		}
		if (!initialized) {
			// What is there already is not announced.
			checkForNewFiles(client, false);
			initialized = true;
			return;
		}
		if (++ticksSincePoll < POLL_INTERVAL_TICKS) {
			return;
		}
		ticksSincePoll = 0;
		checkForNewFiles(client, true);
	}

	private static void checkForNewFiles(MinecraftClient client, boolean announce) {
		File[] files = new File(client.runDirectory, "screenshots").listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (KNOWN_FILE_NAMES.add(file.getName()) && announce) {
				client.inGameHud.getChatHud().addMessage(ScreenshotChatLink.buildChatMessage(file));
			}
		}
	}
}
