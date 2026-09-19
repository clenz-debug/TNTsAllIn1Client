package com.tntsallin1client.screenshot;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Phase 5n, rebuilt on user feedback: a chat message with two colored,
 * bracketed links - "[Open]" and "[Copy]" - instead of the original popup
 * Screen. "Open" is a plain {@code ClickEvent.OpenFile}, the same mechanism
 * vanilla's own screenshot chat message already uses for its filename link.
 * "Copy" can't be a normal ClickEvent - vanilla's fixed action set has no
 * "run arbitrary client code" case, and {@code ClickEvent.CopyToClipboard}
 * only ever copies text, never image data. Instead this uses
 * {@code ClickEvent.Custom} with our own {@link Identifier} and the file
 * path stashed in its NBT payload, following the exact same pattern
 * vanilla's own {@code ChatScreen} already uses internally for one of its
 * own click cases ({@code ChatComponent.QUEUE_EXPAND_ID}, the "expand queued
 * chat messages" link) - {@link com.tntsallin1client.mixin.ChatScreenMixin}
 * intercepts clicks with our id before they'd otherwise fall through to
 * vanilla's default handling (which for {@code Custom} just round-trips
 * through a server packet, useless here).
 */
public final class ScreenshotChatLink {
	public static final Identifier COPY_CLICK_ID = Identifier.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "screenshot_copy");

	private ScreenshotChatLink() {
	}

	public static Component buildChatMessage(File file) {
		MutableComponent openLink = bracketLink(Component.translatable("gui.tntsallin1client.screenshot_toast.open"),
				ChatFormatting.GREEN, new ClickEvent.OpenFile(file));
		MutableComponent copyLink = bracketLink(Component.translatable("gui.tntsallin1client.screenshot_toast.copy"),
				ChatFormatting.BLUE, new ClickEvent.Custom(COPY_CLICK_ID, Optional.of(StringTag.valueOf(file.getAbsolutePath()))));

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

	public static void copyToClipboard(File file) {
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null) {
				TNTsAllIn1ClientMod.LOGGER.warn("[{}] Couldn't read screenshot for clipboard copy: {}", TNTsAllIn1ClientMod.MOD_ID, file);
				return;
			}
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new ImageTransferable(image), null);
		} catch (IOException e) {
			TNTsAllIn1ClientMod.LOGGER.warn("[{}] Failed to copy screenshot to clipboard.", TNTsAllIn1ClientMod.MOD_ID, e);
		}
	}
}
