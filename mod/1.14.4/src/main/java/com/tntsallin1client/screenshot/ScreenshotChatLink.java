package com.tntsallin1client.screenshot;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

/**
 * The chat message for a new screenshot: its name and two colored, bracketed links - "[Open]" and
 * "[Copy]". "Open" is the game's own open-file click. "Copy" has to put the picture itself on the
 * clipboard, which no click action of the game can do: it is a run-command click with a command of
 * our own that `ScreenMixin` takes before the game would send it to the server.
 *
 * <p>Putting a picture on the clipboard is Java's desktop toolkit's job - which this version of the
 * game switches off for itself at its start ("headless", from 1.13 on), and the game's own clipboard
 * only takes text. Where the toolkit is off, Windows is asked through PowerShell instead; on other
 * systems there is no way left, which the log then says.
 */
public final class ScreenshotChatLink {
	/** Followed by the file's path. Never reaches the server. */
	private static final String COPY_COMMAND = "/tntsallin1client-copy-screenshot ";
	private static final int POWERSHELL_TIMEOUT_SECONDS = 15;

	private ScreenshotChatLink() {
	}

	public static Component buildChatMessage(File file) {
		// Our texts are only known to the client's translations, not to the ones a translatable chat
		// text looks its key up in - so they go in translated.
		Component message = new TextComponent(I18n.get("gui.tntsallin1client.screenshot_toast.message", file.getName()));
		message.append(" ");
		message.append(bracketLink("gui.tntsallin1client.screenshot_toast.open", ChatFormatting.GREEN,
				new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
		message.append(" ");
		message.append(bracketLink("gui.tntsallin1client.screenshot_toast.copy", ChatFormatting.BLUE,
				new ClickEvent(ClickEvent.Action.RUN_COMMAND, COPY_COMMAND + file.getAbsolutePath())));
		return message;
	}

	private static Component bracketLink(String labelKey, ChatFormatting color, ClickEvent clickEvent) {
		return new TextComponent("[" + I18n.get(labelKey) + "]")
				.setStyle(new Style().setColor(color).setUnderlined(true).setClickEvent(clickEvent));
	}

	/** @return whether the click was a "[Copy]" link and is dealt with */
	public static boolean handleClick(ClickEvent clickEvent) {
		if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND || !clickEvent.getValue().startsWith(COPY_COMMAND)) {
			return false;
		}
		final File file = new File(clickEvent.getValue().substring(COPY_COMMAND.length()));
		// Reading the picture and waiting for PowerShell both take a moment - not on the game's own thread.
		Thread thread = new Thread(() -> copyToClipboard(file), "TNT screenshot copy");
		thread.setDaemon(true);
		thread.start();
		return true;
	}

	private static void copyToClipboard(File file) {
		try {
			if (!GraphicsEnvironment.isHeadless()) {
				BufferedImage image = ImageIO.read(file);
				if (image == null) {
					TNTsAllIn1ClientMod.LOGGER.warn("Couldn't read the screenshot {} to copy it.", file);
					return;
				}
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(image), null);
			} else if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
				copyThroughPowerShell(file);
			} else {
				TNTsAllIn1ClientMod.LOGGER.warn("Copying a screenshot to the clipboard is not possible on this system.");
			}
		} catch (IOException | RuntimeException e) {
			TNTsAllIn1ClientMod.LOGGER.warn("Failed to copy the screenshot {} to the clipboard.", file, e);
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
			TNTsAllIn1ClientMod.LOGGER.warn("Copying the screenshot {} to the clipboard took too long.", file);
		} else if (process.exitValue() != 0) {
			TNTsAllIn1ClientMod.LOGGER.warn("Copying the screenshot {} to the clipboard failed (PowerShell exit code {}).", file, process.exitValue());
		}
	}
}
