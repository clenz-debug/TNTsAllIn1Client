package com.tntsallin1client.screenshot;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Phase 5n: detects when a screenshot was just taken by watching the same
 * {@code options.keyScreenshot} keybind vanilla's own {@code KeyboardHandler}
 * reacts to. {@code consumeClick()} drains an independent click counter and
 * doesn't stop vanilla's own (separate, raw-key-event-based) handling of the
 * same press, so this coexists safely instead of interfering.
 *
 * <p>The actual file write happens asynchronously on a background thread
 * ({@code Util.ioPool()} inside vanilla's {@code Screenshot.grab}), so this
 * can't assume the PNG exists the instant the key is pressed - it snapshots
 * the screenshots folder's contents, then polls for a few seconds for a
 * filename that wasn't there before. A reasonable approximation for a
 * "moderat" feature rather than hooking the write completion itself, which
 * would mean mixing into a lambda nested inside another lambda inside
 * {@code Screenshot.grab} - a lot more fragile than a short poll.
 */
public final class ScreenshotWatcher {
	private static final int TIMEOUT_TICKS = 100;

	private static Set<String> filesBeforeTrigger;
	private static int ticksWaited;

	private ScreenshotWatcher() {
	}

	public static void tick(Minecraft client) {
		if (filesBeforeTrigger == null) {
			if (ClientConfig.get().screenshotToastEnabled && client.options.keyScreenshot.consumeClick()) {
				filesBeforeTrigger = listScreenshotFileNames(client);
				ticksWaited = 0;
			}
			return;
		}

		ticksWaited++;
		File newFile = findNewFile(client, filesBeforeTrigger);
		if (newFile != null) {
			filesBeforeTrigger = null;
			if (client.screen == null) {
				client.setScreen(new ScreenshotToastScreen(newFile));
			}
		} else if (ticksWaited >= TIMEOUT_TICKS) {
			filesBeforeTrigger = null;
		}
	}

	private static Set<String> listScreenshotFileNames(Minecraft client) {
		Set<String> names = new HashSet<>();
		File[] files = screenshotDir(client).listFiles();
		if (files != null) {
			for (File file : files) {
				names.add(file.getName());
			}
		}
		return names;
	}

	private static File findNewFile(Minecraft client, Set<String> namesBefore) {
		File[] files = screenshotDir(client).listFiles();
		if (files == null) {
			return null;
		}
		for (File file : files) {
			if (!namesBefore.contains(file.getName())) {
				return file;
			}
		}
		return null;
	}

	private static File screenshotDir(Minecraft client) {
		return new File(client.gameDirectory, "screenshots");
	}
}
