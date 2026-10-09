package com.tntsallin1client.design;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * The client design's smooth font - Inter (SIL OFL 1.1, license next to the file in
 * {@code assets/tntsallin1client/font/}), per user request: Minecraft's pixel font looked too blocky
 * next to the client design's clean lines. A TrueType provider ({@code font/client.json}), picked per
 * text via its style, so vanilla screens keep Minecraft's own font.
 */
public final class ClientFont {
	private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("tntsallin1client", "client"));

	private ClientFont() {
	}

	public static MutableComponent of(Component text) {
		return text.copy().withStyle(style -> style.withFont(FONT));
	}

	public static MutableComponent of(String text) {
		return of(Component.literal(text));
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
