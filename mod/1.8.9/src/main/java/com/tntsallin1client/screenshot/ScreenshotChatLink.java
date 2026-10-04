package com.tntsallin1client.screenshot;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The chat message for a new screenshot: its name and two colored, bracketed links - "[Open]" and
 * "[Copy]". "Open" is the game's own open-file click. "Copy" has to put the picture itself on the
 * clipboard, which no click action of the game can do: it is a run-command click with a command of
 * our own that `ScreenMixin` takes before the game would send it to the server.
 */
public final class ScreenshotChatLink {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	/** Followed by the file's path. Never reaches the server. */
	private static final String COPY_COMMAND = "/tntsallin1client-copy-screenshot ";

	private ScreenshotChatLink() {
	}

	public static Text buildChatMessage(File file) {
		// Our texts are only known to the client's translations, not to the ones a translatable chat
		// text looks its key up in - so they go in translated.
		Text message = new LiteralText(I18n.translate("gui.tntsallin1client.screenshot_toast.message", file.getName()));
		message.append(" ");
		message.append(bracketLink("gui.tntsallin1client.screenshot_toast.open", Formatting.GREEN,
				new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
		message.append(" ");
		message.append(bracketLink("gui.tntsallin1client.screenshot_toast.copy", Formatting.BLUE,
				new ClickEvent(ClickEvent.Action.RUN_COMMAND, COPY_COMMAND + file.getAbsolutePath())));
		return message;
	}

	private static Text bracketLink(String labelKey, Formatting color, ClickEvent clickEvent) {
		return new LiteralText("[" + I18n.translate(labelKey) + "]")
				.setStyle(new Style().setFormatting(color).setUnderline(true).setClickEvent(clickEvent));
	}

	/** @return whether the click was a "[Copy]" link and is dealt with */
	public static boolean handleClick(ClickEvent clickEvent) {
		if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND || !clickEvent.getValue().startsWith(COPY_COMMAND)) {
			return false;
		}
		copyToClipboard(new File(clickEvent.getValue().substring(COPY_COMMAND.length())));
		return true;
	}

	private static void copyToClipboard(File file) {
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null) {
				LOGGER.warn("Couldn't read the screenshot " + file + " to copy it.");
				return;
			}
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(image), null);
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to copy the screenshot " + file + " to the clipboard.", e);
		}
	}
}
