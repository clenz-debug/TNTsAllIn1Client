package com.tntsallin1client.screenshot;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Phase 5n, rebuilt on user feedback: a chat message with two colored,
 * bracketed links - "[Open]" and "[Copy]" - instead of the original popup
 * Screen. "Open" is a plain {@code ClickEvent.OpenFile}, the same mechanism
 * vanilla's own screenshot chat message already uses for its filename link.
 * "Copy" can't be a normal ClickEvent - vanilla's fixed action set has no
 * "run arbitrary client code" case, and {@code ClickEvent.CopyToClipboard}
 * only ever copies text, never image data. Instead this uses
 * {@code ClickEvent.Custom} with our own {@link ResourceLocation} and the file
 * path stashed in its NBT payload, following the exact same pattern
 * vanilla's own {@code ChatScreen} already uses internally for one of its
 * own click cases ({@code ChatComponent.QUEUE_EXPAND_ID}, the "expand queued
 * chat messages" link) - {@link com.tntsallin1client.mixin.ChatScreenMixin}
 * intercepts clicks with our id before they'd otherwise fall through to
 * vanilla's default handling (which for {@code Custom} just round-trips
 * through a server packet, useless here).
 */
public final class ScreenshotChatLink {
	private static final int POWERSHELL_TIMEOUT_SECONDS = 15;

	/** No custom click events before 1.21.5 - the link is a command no server knows, caught in {@code ScreenMixin}. */
	private static final String COPY_COMMAND = "/tntsallin1client-copy-screenshot ";

	private ScreenshotChatLink() {
	}

	public static Component buildChatMessage(File file) {
		MutableComponent openLink = bracketLink(Component.translatable("gui.tntsallin1client.screenshot_toast.open"),
				ChatFormatting.GREEN, new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
		MutableComponent copyLink = bracketLink(Component.translatable("gui.tntsallin1client.screenshot_toast.copy"),
				ChatFormatting.BLUE, new ClickEvent(ClickEvent.Action.RUN_COMMAND, COPY_COMMAND + file.getAbsolutePath()));

		return Component.translatable("gui.tntsallin1client.screenshot_toast.message", file.getName())
				.append(" ")
				.append(openLink)
				.append(" ")
				.append(copyLink);
	}

	private static MutableComponent bracketLink(Component label, ChatFormatting color, ClickEvent clickEvent) {
		return Component.literal("[").append(label).append("]")
				.withStyle(style -> style.withColor(color).withUnderlined(true).withClickEvent(clickEvent));
	}

	/** @return whether the click was a "[Copy]" link and is dealt with */
	public static boolean handleClick(@Nullable ClickEvent clickEvent) {
		if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND || !clickEvent.getValue().startsWith(COPY_COMMAND)) {
			return false;
		}
		copyToClipboard(new File(clickEvent.getValue().substring(COPY_COMMAND.length())));
		return true;
	}

	/**
	 * Puts the picture itself on the clipboard, off the game's own thread - reading it and waiting
	 * for PowerShell both take a moment.
	 *
	 * <p>That is Java's desktop toolkit's job, which the game switches off for itself at its start
	 * ("headless") - asking it anyway only throws (own user report: "[Copy]" did nothing). Where the
	 * toolkit is off, Windows is asked through PowerShell instead; on other systems there is no way
	 * left, which the log then says.
	 */
	public static void copyToClipboard(File file) {
		Thread thread = new Thread(() -> copy(file), "TNT screenshot copy");
		thread.setDaemon(true);
		thread.start();
	}

	private static void copy(File file) {
		try {
			if (!GraphicsEnvironment.isHeadless()) {
				BufferedImage image = ImageIO.read(file);
				if (image == null) {
					TNTsAllIn1ClientMod.LOGGER.warn("[{}] Couldn't read screenshot for clipboard copy: {}", TNTsAllIn1ClientMod.MOD_ID, file);
					return;
				}
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(image), null);
			} else if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
				copyThroughPowerShell(file);
			} else {
				TNTsAllIn1ClientMod.LOGGER.warn("[{}] Copying a screenshot to the clipboard is not possible on this system.", TNTsAllIn1ClientMod.MOD_ID);
			}
		} catch (IOException | RuntimeException e) {
			TNTsAllIn1ClientMod.LOGGER.warn("[{}] Failed to copy screenshot to clipboard.", TNTsAllIn1ClientMod.MOD_ID, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Lets Windows load the picture and put it on the clipboard. The path goes in as a single-quoted
	 * PowerShell text - a quote inside it is written twice. The clipboard needs the single-threaded
	 * mode ("-STA").
	 */
	private static void copyThroughPowerShell(File file) throws IOException, InterruptedException {
		String path = file.getAbsolutePath().replace("'", "''");
		String script = "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; "
				+ "$image = [System.Drawing.Image]::FromFile('" + path + "'); "
				+ "[System.Windows.Forms.Clipboard]::SetImage($image); "
				+ "$image.Dispose()";
		Process process = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-STA", "-Command", script)
				.redirectErrorStream(true)
				.start();
		// Nothing is written to it, and what it prints is of no use - closed, so it can't wait on either.
		process.getOutputStream().close();
		process.getInputStream().close();
		if (!process.waitFor(POWERSHELL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			TNTsAllIn1ClientMod.LOGGER.warn("[{}] Copying the screenshot {} to the clipboard took too long.", TNTsAllIn1ClientMod.MOD_ID, file);
		} else if (process.exitValue() != 0) {
			TNTsAllIn1ClientMod.LOGGER.warn("[{}] Copying the screenshot {} to the clipboard failed (PowerShell exit code {}).",
					TNTsAllIn1ClientMod.MOD_ID, file, process.exitValue());
		}
	}
}
