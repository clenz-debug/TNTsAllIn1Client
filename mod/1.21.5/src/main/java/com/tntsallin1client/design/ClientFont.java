package com.tntsallin1client.design;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * The client design's smooth font - Inter (SIL OFL 1.1, license next to the file in
 * {@code assets/tntsallin1client/font/}), per user request: Minecraft's pixel font looked too blocky
 * next to the client design's clean lines. A TrueType provider ({@code font/client.json}), picked per
 * text via its style, so vanilla screens keep Minecraft's own font.
 */
public final class ClientFont {
	private static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("tntsallin1client", "client");
	private static final Style STYLE = Style.EMPTY.withFont(FONT);

	private ClientFont() {
	}

	public static MutableComponent of(Component text) {
		return text.copy().withStyle(style -> style.withFont(FONT));
	}

	public static MutableComponent of(String text) {
		return of(Component.literal(text));
	}

	/** {@code text} with every character switched to this font, keeping the rest of its style. */
	public static FormattedCharSequence of(FormattedCharSequence text) {
		return sink -> text.accept((index, style, codePoint) -> sink.accept(index, style.withFont(FONT), codePoint));
	}

	/** How wide {@code text} is in this font - {@link Font#width(String)} for the game's font. */
	public static int width(Font font, String text) {
		return font.width(of(text));
	}

	/**
	 * The longest start of {@code text} (its end with {@code fromEnd}) that fits {@code maxWidth} in this
	 * font - {@link Font#plainSubstrByWidth(String, int, boolean)} for the game's font.
	 */
	public static String trim(Font font, String text, int maxWidth, boolean fromEnd) {
		StringSplitter splitter = font.getSplitter();
		return fromEnd ? splitter.plainTailByWidth(text, maxWidth, STYLE) : splitter.plainHeadByWidth(text, maxWidth, STYLE);
	}

	/** {@code text} cut down (with an ellipsis) until it fits {@code maxWidth} in this font. */
	public static MutableComponent fit(Font font, String text, int maxWidth) {
		if (font.width(of(text)) <= maxWidth) {
			return of(text);
		}
		String cut = text;
		while (!cut.isEmpty() && font.width(of(cut + "…")) > maxWidth) {
			cut = cut.substring(0, cut.length() - 1);
		}
		return of(cut.strip() + "…");
	}
}
