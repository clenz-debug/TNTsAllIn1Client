package com.tntsallin1client.design;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The octagon "N" client logo, drawn from the exact same shapes as the launcher's {@code Logo.tsx}
 * (600x660 view box; octagon outline, beam outlines and text in background2, the four beams in
 * accent1-4) - so the in-game logo follows the launcher's theme colors the same way (own wishlist
 * item: "Logo anpassen auch an die farben").
 *
 * <p>The shapes are rasterized (anti-aliased, see {@link Shapes#fillConvex}) on the CPU into a
 * texture at the exact pixel size needed, once per size, and then drawn as a single image - drawing
 * them as per-pixel-row fills every frame took tens of thousands of quads per frame and made the
 * title screen noticeably sluggish (own user report: the mouse felt slow over the logo). Only the
 * switch animation, whose logo changes size every frame, rasterizes anew per frame.
 */
public final class ClientLogo {
	/** View-box size of {@code Logo.tsx} - every coordinate below is in these units. */
	public static final float VIEW_WIDTH = 600.0f;
	public static final float VIEW_HEIGHT = 660.0f;

	private static final float[] OCTAGON = {200, 40, 400, 40, 560, 200, 560, 400, 400, 560, 200, 560, 40, 400, 40, 200};
	private static final float[] TOP_BEAM = {150, 150, 450, 150, 450, 200, 150, 200};
	private static final float[] LEFT_LEG = {185, 200, 230, 200, 230, 440, 185, 440};
	private static final float[] RIGHT_LEG = {370, 200, 415, 200, 415, 440, 370, 440};
	private static final float[] DIAGONAL = {185, 200, 230, 200, 415, 415, 415, 440, 370, 440, 185, 225};

	/** Rendered logo sizes kept as textures - resting size, hover size and a few animation frames. */
	private static final int CACHE_SIZE = 6;
	private static final Map<Integer, CachedTexture> CACHE = new LinkedHashMap<>(16, 0.75f, true);

	private record CachedTexture(Identifier id, int width, int height) {
	}

	private ClientLogo() {
	}

	/** Logo width for a given height, keeping the view box's aspect ratio. */
	public static float widthFor(float height) {
		return height * VIEW_WIDTH / VIEW_HEIGHT;
	}

	/** Draws the logo with its top-left corner at ({@code x}, {@code y}) in GUI coordinates, {@code height} tall. */
	public static void draw(GuiGraphics graphics, float x, float y, float height, float alpha) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		CachedTexture texture = texture(Math.max(1, Math.round(height * guiScale)));

		// Pixel-scaled pose, so the texture lands 1:1 on screen pixels - no filtering blur.
		graphics.pose().pushMatrix();
		graphics.pose().scale(1.0f / guiScale, 1.0f / guiScale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id, Math.round(x * guiScale), Math.round(y * guiScale),
				0.0f, 0.0f, texture.width, texture.height, texture.width, texture.height, ARGB.white(alpha));
		graphics.pose().popMatrix();

		int line = ClientTheme.withAlpha(ClientTheme.get().background2, alpha);
		Font font = Minecraft.getInstance().font;
		// Logo.tsx: "A I 1" at font-size 72 (baseline 500), "CLIENT" at 26 (baseline 535) - scaled so
		// the client font's ~7px cap height matches those cap heights (~0.72em). "CLIENT" only once
		// it's big enough to still be legible.
		drawText(graphics, font, "A I 1", x + widthFor(height) / 2, y + height * 448 / VIEW_HEIGHT, height * 52 / VIEW_HEIGHT / 7.0f, line);
		if (height >= 48) {
			drawText(graphics, font, "CLIENT", x + widthFor(height) / 2, y + height * 516 / VIEW_HEIGHT, height * 19 / VIEW_HEIGHT / 7.0f, line);
		}
	}

	/**
	 * An "X" icon of two anti-aliased strokes, exactly centered on ({@code centerX}, {@code centerY}) -
	 * the client layout's quit button (own user report: a text "X" never sat quite centered in its box).
	 * Small enough to draw as fills directly.
	 */
	public static void drawCross(GuiGraphics graphics, float centerX, float centerY, float size, float thickness, int color) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		graphics.pose().pushMatrix();
		graphics.pose().scale(1.0f / guiScale, 1.0f / guiScale);
		Shapes shapes = new Shapes((x0, x1, row, spanColor) -> graphics.fill(x0, row, x1, row + 1, spanColor),
				centerX * guiScale, centerY * guiScale, guiScale);
		float half = size / 2;
		shapes.line(-half, -half, half, half, thickness * guiScale, color);
		shapes.line(-half, half, half, -half, thickness * guiScale, color);
		graphics.pose().popMatrix();
	}

	private static void drawText(GuiGraphics graphics, Font font, String text, float centerX, float top, float scale, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, top);
		graphics.pose().scale(scale, scale);
		var label = ClientFont.of(text);
		graphics.drawString(font, label, -font.width(label) / 2, 0, color, false);
		graphics.pose().popMatrix();
	}

	/** The logo shapes at {@code pixelHeight}, from the cache or freshly rasterized and uploaded. */
	private static CachedTexture texture(int pixelHeight) {
		CachedTexture cached = CACHE.get(pixelHeight);
		if (cached != null) {
			return cached;
		}

		int pixelWidth = Math.max(1, (int) Math.ceil(widthFor(pixelHeight)));
		Shapes.Raster raster = new Shapes.Raster(pixelWidth, pixelHeight);
		Shapes shapes = new Shapes(raster, 0, 0, pixelHeight / VIEW_HEIGHT);
		float outlineWidth = Math.max(1.0f, 4.0f * shapes.unit);
		float beamOutlineWidth = Math.max(1.0f, 3.0f * shapes.unit);
		ClientTheme theme = ClientTheme.get();

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

		NativeImage image = raster.toImage();
		Identifier id = Identifier.fromNamespaceAndPath("tntsallin1client", "dynamic/client_logo_" + pixelHeight);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "TNT's All-In-1 client logo", image));

		CachedTexture created = new CachedTexture(id, pixelWidth, pixelHeight);
		CACHE.put(pixelHeight, created);
		if (CACHE.size() > CACHE_SIZE) {
			Iterator<CachedTexture> eldest = CACHE.values().iterator();
			Minecraft.getInstance().getTextureManager().release(eldest.next().id);
			eldest.remove();
		}
		return created;
	}
}
