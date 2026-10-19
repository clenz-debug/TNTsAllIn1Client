package com.tntsallin1client.screenshot;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Phase 5n: detects new screenshots by polling the {@code screenshots/}
 * folder directly, every {@link #POLL_INTERVAL_TICKS} ticks.
 *
 * <p><b>Bugfix (feedback: "never shows up"):</b> the original version watched
 * {@code options.keyScreenshot.consumeClick()}, the same technique that
 * works fine for this mod's own custom keybinds. That doesn't work for
 * vanilla's screenshot key specifically: {@code KeyboardHandler#keyPress}
 * handles {@code keyScreenshot} as a hardcoded special case that calls
 * {@code Screenshot.grab(...)} and then {@code return}s immediately -
 * before reaching the generic {@code KeyMapping.click(key)} call further
 * down in the same method that would otherwise increment the click counter
 * {@code consumeClick()} drains. So the counter for that specific key never
 * moves, no matter how the screenshot was actually triggered (key or
 * otherwise) - confirmed by reading {@code KeyboardHandler.java} directly.
 * Polling the directory instead sidesteps the problem entirely: it doesn't
 * care how or whether a key was involved, only that a new file showed up.
 */
public final class ScreenshotWatcher {
	private static final int POLL_INTERVAL_TICKS = 10;

	private static boolean initialized = false;
	private static final Set<String> knownFileNames = new HashSet<>();
	private static int ticksSincePoll = 0;

	private ScreenshotWatcher() {
	}

	public static void tick(Minecraft client) {
		if (!ClientConfig.get().screenshotToastEnabled) {
			return;
		}

		if (!initialized) {
			initializeBaseline(client);
			return;
		}

		if (++ticksSincePoll < POLL_INTERVAL_TICKS) {
			return;
		}
		ticksSincePoll = 0;

		checkForNewFiles(client);
	}

	/** Seeds the "already known" set from whatever's on disk right now, so pre-existing screenshots don't all get announced at once. */
	private static void initializeBaseline(Minecraft client) {
		File[] files = screenshotDir(client).listFiles();
		if (files != null) {
			for (File file : files) {
				knownFileNames.add(file.getName());
			}
		}
		initialized = true;
	}

	private static void checkForNewFiles(Minecraft client) {
		File[] files = screenshotDir(client).listFiles();
		if (files == null) {
			return;
		}

		for (File file : files) {
			if (knownFileNames.add(file.getName())) {
				client.gui.getChat().addMessage(ScreenshotChatLink.buildChatMessage(file));
			}
		}
	}

	private static File screenshotDir(Minecraft client) {
		return new File(client.gameDirectory, "screenshots");
	}
}
