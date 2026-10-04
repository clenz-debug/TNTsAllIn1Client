package com.tntsallin1client.design;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/**
 * The octagon "N" client logo, drawn from the exact same shapes as the launcher's `Logo.tsx` and the
 * Fabric versions' logo (600x660 view box; octagon outline, beam outlines and text in background2,
 * the four beams in accent1-4) - so it follows the launcher's theme colors the same way.
 *
 * <p>The shapes are rasterized (anti-aliased, see {@link Shapes}) into a texture at the exact pixel
 * size needed, once per size, and then drawn as a single image. The two lines of text below the "N"
 * are painted into the same picture, at the size the Fabric versions scale their text to. Only the
 * switch animation, whose logo changes size every frame, rasterizes anew per frame.
 */
public final class ClientLogo {
	/** View-box size of `Logo.tsx` - every coordinate below is in these units. */
	public static final float VIEW_WIDTH = 600.0F;
	public static final float VIEW_HEIGHT = 660.0F;

	private static final float[] OCTAGON = {200, 40, 400, 40, 560, 200, 560, 400, 400, 560, 200, 560, 40, 400, 40, 200};
	private static final float[] TOP_BEAM = {150, 150, 450, 150, 450, 200, 150, 200};
	private static final float[] LEFT_LEG = {185, 200, 230, 200, 230, 440, 185, 440};
	private static final float[] RIGHT_LEG = {370, 200, 415, 200, 415, 440, 370, 440};
	private static final float[] DIAGONAL = {185, 200, 230, 200, 415, 415, 415, 440, 370, 440, 185, 225};

	/**
	 * The two lines of text, as the Fabric versions place them: where a line's top is in the view box,
	 * and what a 7 pixel high letter of the client font is scaled to there. That font's line is 9
	 * pixels from top to bottom and sits one pixel down.
	 */
	private static final float NAME_TOP = 448;
	private static final float NAME_LETTER_HEIGHT = 52;
	private static final float CLIENT_TOP = 516;
	private static final float CLIENT_LETTER_HEIGHT = 19;
	private static final float FONT_LETTER_HEIGHT = 7.0F;
	private static final float FONT_LINE_SIZE = 9.0F;
	private static final float FONT_TOP_SHIFT = 1.0F;
	/** "CLIENT" only once the logo is big enough for it to still be legible - in GUI pixels. */
	private static final float CLIENT_LINE_MIN_HEIGHT = 48;

	/** Rendered logo sizes kept as textures - resting size, hover size and a few animation frames. */
	private static final int CACHE_SIZE = 6;
	private static final Map<Integer, CachedTexture> CACHE = new LinkedHashMap<Integer, CachedTexture>(16, 0.75F, true);

	private static final class CachedTexture {
		final Identifier id;
		final int width;
		final int height;

		CachedTexture(Identifier id, int width, int height) {
			this.id = id;
			this.width = width;
			this.height = height;
		}
	}

	private ClientLogo() {
	}

	/** Logo width for a given height, keeping the view box's aspect ratio. */
	public static float widthFor(float height) {
		return height * VIEW_WIDTH / VIEW_HEIGHT;
	}

	/** Draws the logo with its top-left corner at (`x`, `y`) in GUI coordinates, `height` tall. */
	public static void draw(float x, float y, float height, float alpha) {
		MinecraftClient client = MinecraftClient.getInstance();
		int guiScale = new Window(client).getScaleFactor();
		CachedTexture texture = texture(Math.max(1, Math.round(height * guiScale)), height >= CLIENT_LINE_MIN_HEIGHT);

		// Screen pixels, so the texture lands 1:1 on them - no filtering blur.
		GlStateManager.pushMatrix();
		GlStateManager.scale(1.0F / guiScale, 1.0F / guiScale, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
		client.getTextureManager().bindTexture(texture.id);
		DrawableHelper.drawTexture(Math.round(x * guiScale), Math.round(y * guiScale), 0.0F, 0.0F, texture.width, texture.height, texture.width, texture.height);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	/**
	 * An "X" icon of two anti-aliased strokes, exactly centered on (`centerX`, `centerY`) - the client
	 * layout's quit button. Small enough to draw as fills directly.
	 */
	public static void drawCross(float centerX, float centerY, float size, float thickness, int color) {
		int guiScale = new Window(MinecraftClient.getInstance()).getScaleFactor();
		GlStateManager.pushMatrix();
		GlStateManager.scale(1.0F / guiScale, 1.0F / guiScale, 1.0F);
		Shapes shapes = new Shapes(new Shapes.SpanTarget() {
			@Override
			public void span(int x0, int x1, int row, int spanColor) {
				DrawableHelper.fill(x0, row, x1, row + 1, spanColor);
			}
		}, centerX * guiScale, centerY * guiScale, guiScale);
		float half = size / 2;
		shapes.line(-half, -half, half, half, thickness * guiScale, color);
		shapes.line(-half, half, half, -half, thickness * guiScale, color);
		GlStateManager.popMatrix();
	}

	/** The logo at `pixelHeight`, from the cache or freshly rasterized and uploaded. */
	private static CachedTexture texture(int pixelHeight, boolean clientLine) {
		int key = clientLine ? pixelHeight : -pixelHeight;
		CachedTexture cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}

		BufferedImage image = paint(pixelHeight, clientLine, ClientTheme.get());
		MinecraftClient client = MinecraftClient.getInstance();
		Identifier id = client.getTextureManager().registerDynamicTexture("tntsallin1client_logo", new NativeImageBackedTexture(image));
		CachedTexture created = new CachedTexture(id, image.getWidth(), image.getHeight());
		CACHE.put(key, created);
		if (CACHE.size() > CACHE_SIZE) {
			Iterator<CachedTexture> eldest = CACHE.values().iterator();
			client.getTextureManager().close(eldest.next().id);
			eldest.remove();
		}
		return created;
	}

	/** The logo as a picture `pixelHeight` high - nothing here needs the game. */
	static BufferedImage paint(int pixelHeight, boolean clientLine, ClientTheme theme) {
		int pixelWidth = Math.max(1, (int) Math.ceil(widthFor(pixelHeight)));
		Shapes.Raster raster = new Shapes.Raster(pixelWidth, pixelHeight);
		Shapes shapes = new Shapes(raster, 0, 0, pixelHeight / VIEW_HEIGHT);
		float outlineWidth = Math.max(1.0F, 4.0F * shapes.unit);
		float beamOutlineWidth = Math.max(1.0F, 3.0F * shapes.unit);

		// Same paint order as the SVG: each beam filled and then outlined before the next one, so the
		// diagonal (last) covers both legs including their outlines, exactly like N-Logo.svg.
		shapes.stroke(OCTAGON, outlineWidth, theme.background2);
		shapes.fill(TOP_BEAM, theme.accent1);
		shapes.stroke(TOP_BEAM, beamOutlineWidth, theme.background2);
		shapes.fill(LEFT_LEG, theme.accent2);
		shapes.stroke(LEFT_LEG, beamOutlineWidth, theme.background2);
		shapes.fill(RIGHT_LEG, theme.accent3);
		shapes.stroke(RIGHT_LEG, beamOutlineWidth, theme.background2);
		shapes.fill(DIAGONAL, theme.accent4);
		shapes.stroke(DIAGONAL, beamOutlineWidth, theme.background2);

		BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, pixelWidth, pixelHeight, raster.pixels, 0, pixelWidth);

		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setColor(new Color(theme.background2, true));
		paintLine(graphics, "A I 1", pixelWidth, pixelHeight, NAME_TOP, NAME_LETTER_HEIGHT);
		if (clientLine) {
			paintLine(graphics, "CLIENT", pixelWidth, pixelHeight, CLIENT_TOP, CLIENT_LETTER_HEIGHT);
		}
		graphics.dispose();
		return image;
	}

	private static void paintLine(Graphics2D graphics, String text, int pixelWidth, int pixelHeight, float viewTop, float viewLetterHeight) {
		// How many picture pixels one pixel of the client font is, on this line.
		float fontPixel = pixelHeight * viewLetterHeight / VIEW_HEIGHT / FONT_LETTER_HEIGHT;
		Font font = ClientFont.sized(FONT_LINE_SIZE * fontPixel);
		if (font == null) {
			return;
		}
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		float top = pixelHeight * viewTop / VIEW_HEIGHT + FONT_TOP_SHIFT * fontPixel;
		float width = (float) metrics.getStringBounds(text, graphics).getWidth();
		graphics.drawString(text, (pixelWidth - width) / 2.0F, top + font.getLineMetrics(text, graphics.getFontRenderContext()).getAscent());
	}
}
