package com.tntsallin1client.design;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

/**
 * The client design's smooth font - Inter (SIL OFL 1.1, license next to the file in
 * `assets/tntsallin1client/font/`), the same as in the other versions. There the game draws it
 * through a TrueType font provider; this version only knows its own pixel font, so the letters are
 * drawn here: Java's own font code paints them once per GUI scale into one picture, at exactly the
 * size they have on screen, and a text is that picture's letters put next to each other pixel for
 * pixel - no scaling, nothing blurred.
 *
 * <p>Sized like the other versions' provider (`font/client.json`: 9 GUI pixels from the letters'
 * top line to their bottom line), so texts are as big as there. Should the font file not load, texts
 * fall back to the game's font.
 */
public final class ClientFont {
	/** A line's height in GUI pixels, like the game's own font. */
	public static final int HEIGHT = 9;

	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String FONT_FILE = "/assets/tntsallin1client/font/inter.ttf";
	/** From the letters' top line to their bottom line - `font/client.json` in the other versions. */
	private static final float LINE_SIZE = 9.0F;
	/**
	 * How far below a text's y the letters' top line sits. Zero puts a capital letter's middle in the
	 * middle of the 9 pixel line (own user report: with the other versions' one pixel, texts sat too
	 * low in their buttons at every GUI scale).
	 */
	private static final float TOP_SHIFT = 0.0F;
	/** Free pixels around every letter in the picture, so a letter reaching past its own box is not cut off. */
	private static final int PADDING = 2;
	/** Latin letters with accents and umlauts, plus the few other signs our texts use. */
	private static final char FIRST_CHAR = ' ';
	private static final char LAST_CHAR = 0xFF;
	private static final String EXTRA_CHARS = "…↺–—„“”’‘€•←→▲▼";
	private static final char UNKNOWN_CHAR = '?';
	private static final char FORMAT_SIGN = '§';
	private static final String ELLIPSIS = "…";
	private static final FontRenderContext SMOOTH = new FontRenderContext(null, true, true);

	private static Font base;
	private static boolean unavailable;
	/** One picture of all letters per GUI scale and text size. */
	private static final Map<Long, Atlas> ATLASES = new HashMap<Long, Atlas>();

	private ClientFont() {
	}

	/** The font with its letters' top-to-bottom line `lineSize` pixels high, or null if the file didn't load. */
	static Font sized(float lineSize) {
		Font font = base();
		if (font == null) {
			return null;
		}
		LineMetrics metrics = font.deriveFont(100.0F).getLineMetrics("Ag", SMOOTH);
		return font.deriveFont(lineSize * 100.0F / (metrics.getAscent() + metrics.getDescent()));
	}

	private static synchronized Font base() {
		if (base == null && !unavailable) {
			try (InputStream in = ClientFont.class.getResourceAsStream(FONT_FILE)) {
				if (in == null) {
					throw new IOException("not in the mod's jar");
				}
				base = Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (IOException | FontFormatException | RuntimeException e) {
				LOGGER.warn("Failed to load the client font " + FONT_FILE + " - using the game's font.", e);
				unavailable = true;
			}
		}
		return base;
	}

	/** The text's width in GUI pixels. */
	public static int width(String text) {
		return width(text, 1.0F);
	}

	/** The text's width in GUI pixels at `size` times the plain size. */
	public static int width(String text, float size) {
		Atlas atlas = atlas(size);
		if (atlas == null || !atlas.covers(text)) {
			return Math.round(Minecraft.getInstance().font.width(text) * size);
		}
		return (int) Math.ceil(atlas.width(text) / atlas.scale);
	}

	/** Draws the text with its line's top left corner at (`x`, `y`) in GUI pixels; `color` is ARGB. */
	public static void draw(String text, float x, float y, int color) {
		draw(text, x, y, color, 1.0F);
	}

	/** The same at `size` times the plain size - painted at that size, not stretched. */
	public static void draw(String text, float x, float y, int color, float size) {
		Atlas atlas = atlas(size);
		if (atlas == null || !atlas.covers(text)) {
			// A sign this font doesn't have: the whole text in the game's font rather than with a gap.
			GlStateManager.pushMatrix();
			GlStateManager.translatef(x, y, 0.0F);
			GlStateManager.scalef(size, size, 1.0F);
			Minecraft.getInstance().font.draw(text, 0, 0, color);
			GlStateManager.popMatrix();
			return;
		}
		atlas.draw(text, x, y, color);
	}

	public static void drawCentered(String text, float centerX, float y, int color) {
		draw(text, centerX - width(text) / 2.0F, y, color);
	}

	/** `text` cut down (with an ellipsis) until it fits `maxWidth`. */
	public static String fit(String text, int maxWidth) {
		return fit(text, maxWidth, 1.0F);
	}

	public static String fit(String text, int maxWidth, float size) {
		if (width(text, size) <= maxWidth) {
			return text;
		}
		String cut = text;
		while (!cut.isEmpty() && width(cut + ELLIPSIS, size) > maxWidth) {
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut.trim() + ELLIPSIS;
	}

	/** As much of `text` as fits `maxWidth` - from its start, or (`fromEnd`) from its end. The game's own font has the same for its text fields. */
	public static String trim(String text, int maxWidth, boolean fromEnd) {
		int length = text.length();
		for (int kept = length; kept > 0; kept--) {
			String part = fromEnd ? text.substring(length - kept) : text.substring(0, kept);
			if (width(part) <= maxWidth) {
				return part;
			}
		}
		return "";
	}

	private static Atlas atlas(float size) {
		int scale = GuiTextures.guiScale();
		long key = ((long) scale << 32) | Float.floatToIntBits(size);
		Atlas atlas = ATLASES.get(key);
		if (atlas == null) {
			Font font = sized(LINE_SIZE * size * scale);
			if (font == null) {
				return null;
			}
			atlas = new Atlas(font, scale, size);
			ATLASES.put(key, atlas);
		}
		return atlas;
	}

	/** Where a letter sits in the picture, and how far the next one stands from it. */
	private static final class Glyph {
		final int x;
		final int y;
		final int width;
		final float advance;

		Glyph(int x, int y, int width, float advance) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.advance = advance;
		}
	}

	private static final class Atlas {
		private static final int PICTURE_WIDTH = 1024;

		final int scale;
		/** How many times the plain size the letters are. */
		private final float size;
		private final Map<Character, Glyph> glyphs = new HashMap<Character, Glyph>();
		private final ResourceLocation texture;
		private final int pictureHeight;
		/** A letter's box in the picture: from its top line to its bottom line, plus the padding. */
		private final int rowHeight;
		/** How far below its top line the font's base line is, exactly - and in the picture, where it is a whole pixel. */
		private final float ascent;
		private final int paintedAscent;

		Atlas(Font font, int scale, float size) {
			this.scale = scale;
			this.size = size;
			StringBuilder chars = new StringBuilder();
			for (char c = FIRST_CHAR; c <= LAST_CHAR; c++) {
				if (font.canDisplay(c)) {
					chars.append(c);
				}
			}
			for (char c : EXTRA_CHARS.toCharArray()) {
				if (font.canDisplay(c)) {
					chars.append(c);
				}
			}

			LineMetrics metrics = font.getLineMetrics(chars.toString(), SMOOTH);
			this.ascent = metrics.getAscent();
			this.paintedAscent = (int) Math.ceil(this.ascent);
			this.rowHeight = this.paintedAscent + (int) Math.ceil(metrics.getDescent()) + 2 * PADDING;

			// First where every letter goes, then the picture as high as that needs.
			float[] advances = new float[chars.length()];
			int x = 0;
			int y = 0;
			for (int i = 0; i < chars.length(); i++) {
				advances[i] = (float) font.getStringBounds(String.valueOf(chars.charAt(i)), SMOOTH).getWidth();
				int width = (int) Math.ceil(advances[i]) + 2 * PADDING;
				if (x + width > PICTURE_WIDTH) {
					x = 0;
					y += this.rowHeight;
				}
				this.glyphs.put(chars.charAt(i), new Glyph(x, y, width, advances[i]));
				x += width;
			}
			this.pictureHeight = y + this.rowHeight;

			BufferedImage image = new BufferedImage(PICTURE_WIDTH, this.pictureHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			graphics.setFont(font);
			graphics.setColor(Color.WHITE);
			for (int i = 0; i < chars.length(); i++) {
				Glyph glyph = this.glyphs.get(chars.charAt(i));
				graphics.drawString(String.valueOf(chars.charAt(i)), glyph.x + PADDING, glyph.y + PADDING + this.paintedAscent);
			}
			graphics.dispose();
			this.texture = GuiTextures.register("tntsallin1client_font", image);
		}

		/** Whether the font has every sign of the text. */
		boolean covers(String text) {
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == FORMAT_SIGN && i + 1 < text.length()) {
					i++;
				} else if (!this.glyphs.containsKey(c)) {
					return false;
				}
			}
			return true;
		}

		private Glyph glyph(char c) {
			Glyph glyph = this.glyphs.get(c);
			return glyph != null ? glyph : this.glyphs.get(UNKNOWN_CHAR);
		}

		/** In screen pixels. */
		float width(String text) {
			float width = 0;
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == FORMAT_SIGN && i + 1 < text.length()) {
					// The game's own color and style codes: this font has neither, so they are left out.
					i++;
					continue;
				}
				Glyph glyph = glyph(c);
				if (glyph != null) {
					width += glyph.advance;
				}
			}
			return width;
		}

		void draw(String text, float x, float y, int color) {
			float alpha = (color >>> 24) / 255.0F;
			if (alpha <= 0) {
				return;
			}
			GlStateManager.pushMatrix();
			// Screen pixels instead of GUI pixels, so every letter lands exactly on the pixels it was painted for.
			GlStateManager.scalef(1.0F / this.scale, 1.0F / this.scale, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
			GlStateManager.enableTexture();
			GlStateManager.color4f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
			Minecraft.getInstance().getTextureManager().bind(this.texture);

			float pen = Math.round(x * this.scale);
			// The base line goes to the screen pixel nearest to where it belongs. Rounding the line's top
			// and the base line's place in the picture each on their own put small texts up to a pixel
			// and a half too low (own user report, at the smallest GUI scale).
			int top = Math.round((y + TOP_SHIFT * this.size) * this.scale + this.ascent) - this.paintedAscent - PADDING;
			Tesselator tessellator = Tesselator.getInstance();
			BufferBuilder buffer = tessellator.getBuilder();
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX);
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == FORMAT_SIGN && i + 1 < text.length()) {
					i++;
					continue;
				}
				Glyph glyph = glyph(c);
				if (glyph == null) {
					continue;
				}
				int left = Math.round(pen) - PADDING;
				float u1 = glyph.x / (float) PICTURE_WIDTH;
				float u2 = (glyph.x + glyph.width) / (float) PICTURE_WIDTH;
				float v1 = glyph.y / (float) this.pictureHeight;
				float v2 = (glyph.y + this.rowHeight) / (float) this.pictureHeight;
				buffer.vertex(left, top + this.rowHeight, 0.0).uv(u1, v2).endVertex();
				buffer.vertex(left + glyph.width, top + this.rowHeight, 0.0).uv(u2, v2).endVertex();
				buffer.vertex(left + glyph.width, top, 0.0).uv(u2, v1).endVertex();
				buffer.vertex(left, top, 0.0).uv(u1, v1).endVertex();
				pen += glyph.advance;
			}
			tessellator.end();
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.popMatrix();
		}
	}
}
