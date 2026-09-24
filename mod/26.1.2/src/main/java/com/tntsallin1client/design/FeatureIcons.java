package com.tntsallin1client.design;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The per-feature symbols on the Client Mods cards ({@code ClientModsCardScreen}), after the user's
 * own sketches - drawn as vector shapes in the theme colors on a 100x100 view box, rasterized
 * anti-aliased once per pixel size like {@link ClientLogo}. Item counter, lightlevel overlay,
 * connected textures and 3D block models were left to us to design
 * ("überleg dir ein Symbol"). Features
 * without a symbol here (sketched as "kein Symbol", or not drawn yet) get their name large instead.
 *
 * <p>Keyed by the feature's menu translation key, so {@code ClientMenuFeatures} stays untouched.
 */
public final class FeatureIcons {
	private static final String MENU = "gui.tntsallin1client.menu.";
	/** Stroke width in view-box units - about 2 GUI pixels at the card's icon size. */
	private static final float W = 7.0f;

	private static final Map<String, BiConsumer<Shapes, ClientTheme>> ICONS = new HashMap<>();
	/** Text drawn on top of an icon, in the client font - the item counter's stack count. */
	private static final Map<String, String> OVERLAY_TEXT = Map.of(MENU + "item_counter", "64");
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();

	static {
		ICONS.put(MENU + "coordinates_hud", FeatureIcons::compass);
		ICONS.put(MENU + "item_counter", FeatureIcons::itemStack);
		ICONS.put(MENU + "fps_counter", FeatureIcons::gauge);
		ICONS.put(MENU + "latency_hud", FeatureIcons::signalBars);
		ICONS.put(MENU + "keystrokes", FeatureIcons::arrowKeys);
		ICONS.put(MENU + "armor_status", FeatureIcons::armorAndTool);
		ICONS.put(MENU + "zoom", FeatureIcons::binoculars);
		ICONS.put(MENU + "freecam", FeatureIcons::figureWithCamera);
		ICONS.put(MENU + "crosshair", FeatureIcons::crosshair);
		ICONS.put(MENU + "fullbright", FeatureIcons::lightBulb);
		ICONS.put(MENU + "spawn_overlay", FeatureIcons::lightLevelGrid);
		ICONS.put(MENU + "hitbox_color", FeatureIcons::hitbox);
		ICONS.put(MENU + "block_outline_color", FeatureIcons::blockOutline);
		ICONS.put(MENU + "item_tilt", FeatureIcons::fallingItem);
		ICONS.put(MENU + "waypoints", FeatureIcons::waypoint);
		ICONS.put(MENU + "connected_textures", FeatureIcons::connectedGlass);
		ICONS.put(MENU + "block_models_3d", FeatureIcons::flatToCube);
		ICONS.put(MENU + "shulker_preview", FeatureIcons::shulkerBox);
		ICONS.put(MENU + "pinned_recipe", FeatureIcons::pinnedRecipe);
		ICONS.put(MENU + "discord_presence", FeatureIcons::discordLogo);
	}

	private FeatureIcons() {
	}

	public static boolean has(String key) {
		return ICONS.containsKey(key);
	}

	/** Draws {@code key}'s symbol {@code size} GUI pixels square with its top-left at ({@code x}, {@code y}). */
	public static void draw(GuiGraphicsExtractor graphics, Font font, String key, int x, int y, int size) {
		BiConsumer<Shapes, ClientTheme> icon = ICONS.get(key);
		if (icon == null) {
			return;
		}
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		int pixels = size * guiScale;
		Identifier id = TEXTURES.computeIfAbsent(key + "@" + pixels, cacheKey -> {
			Shapes.Raster raster = new Shapes.Raster(pixels, pixels);
			icon.accept(new Shapes(raster, 0, 0, pixels / 100.0f).roundJoins(), ClientTheme.get());
			Identifier created = Identifier.fromNamespaceAndPath("tntsallin1client",
					"dynamic/feature_icon_" + key.substring(MENU.length()) + "_" + pixels);
			Minecraft.getInstance().getTextureManager().register(created, new DynamicTexture(() -> "Feature icon " + cacheKey, raster.toImage()));
			return created;
		});

		// Pixel-scaled pose, so the texture lands 1:1 on screen pixels - no filtering blur.
		graphics.pose().pushMatrix();
		graphics.pose().scale(1.0f / guiScale, 1.0f / guiScale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, id, x * guiScale, y * guiScale, 0.0f, 0.0f, pixels, pixels, pixels, pixels, ARGB.white(1.0f));
		graphics.pose().popMatrix();

		String overlay = OVERLAY_TEXT.get(key);
		if (overlay != null) {
			Component text = ClientFont.of(overlay);
			// Inside the slot's lower right corner, where Minecraft puts stack counts - scaled down, with the
			// same gap to the frame on the right and at the bottom (measured from an in-game screenshot: the
			// width includes a trailing advance, the digits' bottom sits about 8 font units below the top)
			float scale = 0.8f;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + size * 0.855f - font.width(text) * scale, y + size * 0.815f - 8 * scale);
			graphics.pose().scale(scale, scale);
			graphics.text(font, text, 0, 0, ClientTheme.get().text, false);
			graphics.pose().popMatrix();
		}
	}

	// --- Shape helpers (view-box units) ------------------------------------------------------

	private static float[] rect(float x0, float y0, float x1, float y1) {
		return new float[] {x0, y0, x1, y0, x1, y1, x0, y1};
	}

	/** Points along a circle arc, angles in degrees (0 = right, 90 = down - screen coordinates). */
	private static float[] arc(float cx, float cy, float r, float fromDegrees, float toDegrees, int segments) {
		float[] points = new float[(segments + 1) * 2];
		for (int i = 0; i <= segments; i++) {
			double angle = Math.toRadians(fromDegrees + (toDegrees - fromDegrees) * i / segments);
			points[i * 2] = cx + r * (float) Math.cos(angle);
			points[i * 2 + 1] = cy + r * (float) Math.sin(angle);
		}
		return points;
	}

	private static float[] circle(float cx, float cy, float r) {
		float[] closed = arc(cx, cy, r, 0, 360, 48);
		float[] points = new float[closed.length - 2];
		System.arraycopy(closed, 0, points, 0, points.length);
		return points;
	}

	/** A straight bar of {@code width} from one point to another, as a convex polygon - for filled tubes and beams. */
	private static float[] bar(float x0, float y0, float x1, float y1, float width) {
		float length = (float) Math.hypot(x1 - x0, y1 - y0);
		float nx = -(y1 - y0) / length * width / 2;
		float ny = (x1 - x0) / length * width / 2;
		return new float[] {x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny};
	}

	private static float[] ellipse(float cx, float cy, float rx, float ry) {
		float[] points = circle(0, 0, 1);
		for (int i = 0; i < points.length; i += 2) {
			points[i] = cx + points[i] * rx;
			points[i + 1] = cy + points[i + 1] * ry;
		}
		return points;
	}

	/** {@code points} (relative to the origin) rotated by {@code degrees} and moved to ({@code cx}, {@code cy}). */
	private static float[] rotated(float[] points, float cx, float cy, float degrees) {
		float cos = (float) Math.cos(Math.toRadians(degrees));
		float sin = (float) Math.sin(Math.toRadians(degrees));
		float[] result = new float[points.length];
		for (int i = 0; i < points.length; i += 2) {
			result[i] = cx + points[i] * cos - points[i + 1] * sin;
			result[i + 1] = cy + points[i] * sin + points[i + 1] * cos;
		}
		return result;
	}

	/** Line with a filled triangular head at its end. */
	private static void arrow(Shapes s, float x0, float y0, float x1, float y1, float head, int color) {
		float length = (float) Math.hypot(x1 - x0, y1 - y0);
		float dx = (x1 - x0) / length;
		float dy = (y1 - y0) / length;
		s.line(x0, y0, x1 - dx * head * 0.8f, y1 - dy * head * 0.8f, W * s.unit, color);
		s.fill(new float[] {
				x1, y1,
				x1 - dx * head - dy * head * 0.6f, y1 - dy * head + dx * head * 0.6f,
				x1 - dx * head + dy * head * 0.6f, y1 - dy * head - dx * head * 0.6f
		}, color);
	}

	/** Wireframe box: front face plus the top and right faces going back by ({@code depthX}, -{@code depthY}). */
	private static void box(Shapes s, float x0, float y0, float x1, float y1, float depthX, float depthY, float width, int color) {
		s.stroke(rect(x0, y0, x1, y1), width, color);
		s.stroke(new float[] {x0, y0, x0 + depthX, y0 - depthY, x1 + depthX, y0 - depthY, x1, y0}, width, color);
		s.stroke(new float[] {x1, y0, x1 + depthX, y0 - depthY, x1 + depthX, y1 - depthY, x1, y1}, width, color);
	}

	// --- Icons -------------------------------------------------------------------------------

	/** Koordinaten-HUD: compass rose, north needle in the accent color. */
	private static void compass(Shapes s, ClientTheme t) {
		float w = W * s.unit;
		s.stroke(circle(50, 50, 42), w, t.text);
		s.line(50, 8, 50, 16, w, t.text);
		s.line(50, 84, 50, 92, w, t.text);
		s.line(8, 50, 16, 50, w, t.text);
		s.line(84, 50, 92, 50, w, t.text);
		s.fill(new float[] {50, 20, 59, 50, 41, 50}, t.accent4);
		s.fill(new float[] {41, 50, 59, 50, 50, 80}, t.text);
	}

	/** Item counter (own design): an inventory slot holding a gem, with a stack count over it. */
	private static void itemStack(Shapes s, ClientTheme t) {
		s.stroke(rect(8, 8, 92, 92), W * s.unit, t.text);
		// Gem up and to the left, leaving the lower right free for the count - like an item in a real slot
		float[] gem = {32, 21, 54, 21, 64, 33, 43, 59, 22, 33};
		s.fill(gem, t.accent4);
		s.stroke(gem, 4 * s.unit, t.text);
	}

	/** FPS counter: speedometer with a needle ("Tacho mit Nadel"). */
	private static void gauge(Shapes s, ClientTheme t) {
		float w = W * s.unit;
		s.polyline(arc(50, 66, 42, 180, 360, 32), w, t.text);
		s.line(8, 66, 92, 66, w, t.text);
		for (int angle = 210; angle <= 330; angle += 30) {
			double radians = Math.toRadians(angle);
			float cos = (float) Math.cos(radians);
			float sin = (float) Math.sin(radians);
			s.line(50 + cos * 30, 66 + sin * 30, 50 + cos * 42, 66 + sin * 42, 5 * s.unit, t.text);
		}
		s.line(50, 66, 50 + 34 * (float) Math.cos(Math.toRadians(305)), 66 + 34 * (float) Math.sin(Math.toRadians(305)), w, t.accent4);
		s.fill(circle(50, 66, 7), t.text);
	}

	/** Latency: signal-strength bars, the last one only outlined. */
	private static void signalBars(Shapes s, ClientTheme t) {
		float stroke = 4;
		for (int i = 0; i < 5; i++) {
			float x0 = 8 + i * 18;
			float top = 84 - 14 - i * 15;
			if (i < 4) {
				s.fill(rect(x0, top, x0 + 12, 84), t.text);
			} else {
				// Inset by half the stroke, so the outline's outer edge lines up with the filled bars
				float inset = stroke / 2;
				s.stroke(rect(x0 + inset, top + inset, x0 + 12 - inset, 84 - inset), stroke * s.unit, t.text);
			}
		}
	}

	/** Keystrokes: arrow-key cluster. */
	private static void arrowKeys(Shapes s, ClientTheme t) {
		float w = 5 * s.unit;
		float size = 28;
		float[][] keys = {{36, 20}, {4, 52}, {36, 52}, {68, 52}};
		for (float[] key : keys) {
			s.stroke(rect(key[0], key[1], key[0] + size, key[1] + size), w, t.text);
		}
		// Up, left, down, right triangles, each centered in its key
		s.fill(new float[] {50, 27, 58, 39, 42, 39}, t.accent4);
		s.fill(new float[] {11, 66, 23, 58, 23, 74}, t.accent4);
		s.fill(new float[] {42, 61, 58, 61, 50, 73}, t.accent4);
		s.fill(new float[] {89, 66, 77, 58, 77, 74}, t.accent4);
	}

	/** Armor/tool status: chestplate and pickaxe, each with a durability bar under it. */
	private static void armorAndTool(Shapes s, ClientTheme t) {
		float w = 5 * s.unit;
		// Chestplate after Minecraft's own item: blocky, on its 14x13 pixel grid - broad shoulders with
		// a neck cut, short arms joined to the narrower body
		float[] chestplate = {0, 0, 5, 0, 5, 2, 9, 2, 9, 0, 14, 0, 14, 7, 11, 7, 11, 13, 3, 13, 3, 7, 0, 7};
		for (int i = 0; i < chestplate.length; i += 2) {
			chestplate[i] = 1 + chestplate[i] * 3.5f;
			chestplate[i + 1] = 31 + chestplate[i + 1] * 3.5f;
		}
		s.fillEvenOdd(new float[][] {chestplate}, t.text);
		// Pickaxe at 45 degrees like Minecraft's own item: handle from the lower left up to a head
		// bent back around its top
		float handleX = 88;
		float handleY = 40;
		float dirX = 0.7071f;
		float dirY = -0.7071f;
		s.line(54, 74, handleX, handleY, w * 1.3f, t.text);
		float[] head = new float[9 * 2];
		for (int i = 0; i <= 8; i++) {
			float u = i / 4.0f - 1;
			float along = 2 - 10 * u * u;
			head[i * 2] = handleX - dirY * 20 * u + dirX * along;
			head[i * 2 + 1] = handleY + dirX * 20 * u + dirY * along;
		}
		s.polyline(head, w * 1.3f, t.text);
		// Durability bars
		s.fill(rect(8, 86, 44, 93), ClientTheme.withAlpha(t.text, 0.3f));
		s.fill(rect(8, 86, 37, 93), t.accent4);
		s.fill(rect(56, 86, 96, 93), ClientTheme.withAlpha(t.text, 0.3f));
		s.fill(rect(56, 86, 90, 93), t.accent4);
	}

	/** Zoom: binoculars ("Fernglas"), lenses in the accent color. */
	private static void binoculars(Shapes s, ClientTheme t) {
		float w = 5 * s.unit;
		for (float center : new float[] {28, 72}) {
			s.stroke(rect(center - 8, 10, center + 8, 24), w, t.text);
			s.stroke(new float[] {center - 11, 24, center + 11, 24, center + 15, 54, center - 15, 54}, w, t.text);
			s.fill(circle(center, 70, 17), ClientTheme.withAlpha(t.accent4, 0.6f));
			s.stroke(circle(center, 70, 20), w, t.text);
		}
		s.fill(rect(40, 32, 60, 44), t.text);
	}

	/** Freecam: Minecraft figure, video camera at the top right. */
	private static void figureWithCamera(Shapes s, ClientTheme t) {
		float w = 4 * s.unit;
		s.stroke(rect(20, 8, 36, 24), w, t.text);
		s.stroke(rect(20, 24, 36, 56), w, t.text);
		s.stroke(rect(12, 24, 20, 56), w, t.text);
		s.stroke(rect(36, 24, 44, 56), w, t.text);
		s.stroke(rect(20, 56, 28, 90), w, t.text);
		s.stroke(rect(28, 56, 36, 90), w, t.text);
		// Camera tilted so its lens points down-left at the figure
		s.fill(rotated(rect(-11, -8, 11, 8), 84, 19, -30), t.accent4);
		s.fill(rotated(new float[] {-8, 0, -21, -9, -21, 9}, 84, 19, -30), t.accent4);
	}

	/** Crosshair. */
	private static void crosshair(Shapes s, ClientTheme t) {
		float w = W * s.unit;
		s.stroke(circle(50, 50, 28), w, t.text);
		s.line(8, 50, 92, 50, w, t.text);
		s.line(50, 8, 50, 92, w, t.text);
		s.fill(circle(50, 50, 5), t.accent4);
	}

	/** Fullbright: glowing light bulb ("Glühbirne"). */
	private static void lightBulb(Shapes s, ClientTheme t) {
		float w = 6 * s.unit;
		s.fill(circle(50, 38, 27), ClientTheme.withAlpha(t.accent4, 0.45f));
		s.polyline(arc(50, 38, 30, 115, 425, 40), w, t.text);
		s.line(37, 65, 40, 74, w, t.text);
		s.line(63, 65, 60, 74, w, t.text);
		s.polyline(new float[] {40, 46, 44, 36, 50, 46, 56, 36, 60, 46}, 4 * s.unit, t.text);
		s.stroke(rect(38, 74, 62, 90), w, t.text);
		s.line(38, 82, 62, 82, 4 * s.unit, t.text);
		s.fill(rect(45, 90, 55, 96), t.text);
	}

	/**
	 * Lightlevel overlay (own design): a 2x2 block floor seen from above at an angle, a torch on the
	 * lit block and flat X marks on the dark, spawnable ones - like the overlay itself.
	 */
	private static void lightLevelGrid(Shapes s, ClientTheme t) {
		float w = 4 * s.unit;
		s.stroke(new float[] {6, 66, 50, 44, 94, 66, 50, 88}, w, t.text);
		s.line(28, 55, 72, 77, w, t.text);
		s.line(72, 55, 28, 77, w, t.text);
		// X marks lying flat on the right and top blocks - corner to corner along each block's
		// diagonals, which run horizontally and vertically on screen in this view
		for (float[] center : new float[][] {{72, 66}, {50, 55}}) {
			s.line(center[0] - 12, center[1], center[0] + 12, center[1], w * 1.2f, t.accent4);
			s.line(center[0], center[1] - 6, center[0], center[1] + 6, w * 1.2f, t.accent4);
		}
		// Torch on the left block: stick, glow and flame
		s.fill(circle(28, 26, 16), ClientTheme.withAlpha(t.accent4, 0.3f));
		s.line(28, 36, 28, 64, 7 * s.unit, t.text);
		s.fill(new float[] {28, 14, 34, 24, 32, 32, 24, 32, 22, 24}, t.accent4);
	}

	/** Hitbox: tall entity box with the look-direction line out of it. */
	private static void hitbox(Shapes s, ClientTheme t) {
		box(s, 38, 32, 62, 92, 14, 12, 5 * s.unit, t.text);
		arrow(s, 52, 30, 12, 12, 14, t.accent4);
	}

	/** Block outline color: a wireframe block. */
	private static void blockOutline(Shapes s, ClientTheme t) {
		box(s, 14, 34, 66, 86, 20, 20, W * s.unit, t.text);
	}

	/** Item physics: a small block falling to the ground, motion lines behind it. */
	private static void fallingItem(Shapes s, ClientTheme t) {
		float w = 5 * s.unit;
		box(s, 54, 56, 80, 82, 10, 10, w, t.text);
		for (int line = 0; line < 3; line++) {
			// Opaque on purpose - with round joins, a translucent polyline shows darker dots where its segments overlap
			s.polyline(arc(6, 90, 46 + line * 11, 272, 318, 12), 4 * s.unit, t.accent4);
		}
		s.line(40, 92, 96, 92, w, t.text);
	}

	/** Waypoints: a beam into a marker block with a distance label. */
	private static void waypoint(Shapes s, ClientTheme t) {
		s.fill(rect(44, 2, 56, 55), ClientTheme.withAlpha(t.accent4, 0.35f));
		s.fill(rect(47, 2, 53, 55), t.accent4);
		box(s, 30, 60, 62, 92, 10, 10, 5 * s.unit, t.text);
		s.line(37, 71, 55, 71, 4 * s.unit, t.text);
		s.line(37, 81, 50, 81, 4 * s.unit, t.text);
	}

	/**
	 * Connected textures (own design): before/after like the 3D block models icon - four single glass
	 * blocks with their seams, then the same area as one pane without inner edges, a shine across it.
	 */
	private static void connectedGlass(Shapes s, ClientTheme t) {
		float w = 4 * s.unit;
		s.stroke(rect(3, 38, 35, 70), w, t.text);
		s.line(19, 38, 19, 70, w, t.text);
		s.line(3, 54, 35, 54, w, t.text);
		arrow(s, 40, 54, 56, 54, 13, t.accent4);
		// Shine edge to edge - the ends sit on the frame's centerline, so the frame drawn over them cuts them off cleanly
		s.line(62, 72, 96, 38, 3.5f * s.unit, t.accent4);
		s.line(62, 61, 85, 38, 2.5f * s.unit, t.accent4);
		s.line(73, 72, 96, 49, 2.5f * s.unit, t.accent4);
		s.stroke(rect(62, 38, 96, 72), w * 1.3f, t.text);
	}

	/** 3D block models (own design): a flat tile turning into a block. */
	private static void flatToCube(Shapes s, ClientTheme t) {
		float w = 5 * s.unit;
		s.stroke(rect(4, 40, 32, 68), w, t.text);
		arrow(s, 37, 54, 53, 54, 13, t.accent4);
		box(s, 58, 44, 86, 72, 10, 10, w, t.text);
	}

	/**
	 * Shulker box preview: a shulker box seen from the front, after the user's reference icon - the lid
	 * reaching down at both sides like legs, the base as an upside-down T fitting between them, a
	 * dark seam where the two meet.
	 */
	private static void shulkerBox(Shapes s, ClientTheme t) {
		// Lid and base each as one outline - separate rectangles left a faint anti-aliasing seam where they met
		s.fillEvenOdd(new float[][] {{8, 14, 92, 14, 92, 66, 74, 66, 74, 48, 26, 48, 26, 66, 8, 66}}, t.text);
		s.fillEvenOdd(new float[][] {{32, 54, 68, 54, 68, 72, 92, 72, 92, 88, 8, 88, 8, 72, 32, 72}}, t.text);
	}

	/** Pinned recipes: a sheet of paper with a crafting grid, a thumbtack stuck into its top edge. */
	private static void pinnedRecipe(Shapes s, ClientTheme t) {
		s.stroke(rect(14, 24, 86, 96), 5 * s.unit, t.text);
		s.stroke(rect(29, 42, 71, 84), 4 * s.unit, t.text);
		s.line(43, 42, 43, 84, 3 * s.unit, t.text);
		s.line(57, 42, 57, 84, 3 * s.unit, t.text);
		s.line(29, 56, 71, 56, 3 * s.unit, t.text);
		s.line(29, 70, 71, 70, 3 * s.unit, t.text);
		// Thumbtack from the side: needle into the paper, flat plate, narrow neck, wide head
		float round = 2 * s.unit;
		s.line(50, 26, 50, 34, 3 * s.unit, t.text);
		float[][] tack = {rect(37, 20, 63, 25), rect(44, 11, 56, 20), rect(39, 4, 61, 11)};
		for (float[] part : tack) {
			s.fill(part, t.accent4);
			s.stroke(part, round, t.accent4);
		}
	}

	/**
	 * Discord activity: Discord's logo mark, traced from its official SVG path (curves flattened to
	 * points, fitted into the view box) - outline plus the two eyes as holes.
	 */
	private static void discordLogo(Shapes s, ClientTheme t) {
		s.fillEvenOdd(DISCORD_LOGO, t.text);
	}

	private static final float[][] DISCORD_LOGO = {
		{
			79.3f, 19.8f, 76.7f, 18.8f, 74, 17.9f, 71.3f, 17, 68.5f, 16.3f, 65.7f, 15.7f, 63, 15.1f, 61.7f, 17.6f,
			60.5f, 20.1f, 57.5f, 19.7f, 54.5f, 19.4f, 51.5f, 19.3f, 48.5f, 19.3f, 45.5f, 19.4f, 42.5f, 19.7f, 39.5f, 20.1f,
			38.3f, 17.6f, 37, 15.1f, 34.2f, 15.7f, 31.5f, 16.3f, 28.7f, 17, 26, 17.9f, 23.3f, 18.8f, 20.6f, 19.9f,
			18, 21, 16.2f, 23.8f, 14.5f, 26.6f, 13, 29.4f, 11.6f, 32.2f, 10.3f, 34.9f, 9.1f, 37.7f, 8.1f, 40.5f,
			7.2f, 43.2f, 6.4f, 46, 5.8f, 48.7f, 5.2f, 51.5f, 4.8f, 54.2f, 4.4f, 56.9f, 4.2f, 59.6f, 4, 62.4f,
			4, 65.1f, 4, 67.8f, 4.2f, 70.5f, 4.4f, 73.2f, 6.8f, 74.9f, 9.2f, 76.5f, 11.7f, 78, 14.2f, 79.4f,
			16.8f, 80.7f, 19.5f, 81.9f, 22.2f, 83, 24.9f, 84, 27.7f, 84.9f, 29.5f, 82.3f, 31.1f, 79.6f, 32.7f, 76.8f,
			30, 75.7f, 27.3f, 74.5f, 24.8f, 73.1f, 26.7f, 71.6f, 29.3f, 72.8f, 32, 73.8f, 34.7f, 74.6f, 37.4f, 75.4f,
			40.2f, 75.9f, 42.9f, 76.4f, 45.8f, 76.7f, 48.6f, 76.8f, 51.4f, 76.8f, 54.2f, 76.7f, 57.1f, 76.4f, 59.8f, 75.9f,
			62.6f, 75.4f, 65.3f, 74.6f, 68, 73.8f, 70.7f, 72.8f, 73.3f, 71.6f, 75.2f, 73.1f, 72.6f, 74.5f, 70, 75.7f,
			67.3f, 76.8f, 68.8f, 79.6f, 70.5f, 82.3f, 72.3f, 84.9f, 75.1f, 84, 77.8f, 83, 80.5f, 81.9f, 83.2f, 80.7f,
			85.8f, 79.4f, 88.3f, 78, 90.8f, 76.5f, 93.2f, 74.9f, 95.6f, 73.2f, 95.9f, 70.1f, 96, 67, 96, 64,
			95.9f, 61, 95.7f, 58.1f, 95.4f, 55.2f, 94.9f, 52.3f, 94.4f, 49.5f, 93.8f, 46.7f, 93, 44, 92.2f, 41.3f,
			91.2f, 38.6f, 90.2f, 36, 89, 33.4f, 87.8f, 30.9f, 86.5f, 28.3f, 85, 25.8f, 83.5f, 23.4f, 81.9f, 21
		},
		{
			32.1f, 62.2f, 29.8f, 60.9f, 28, 58.9f, 26.9f, 56.4f, 26.4f, 53.5f, 26.8f, 50.6f, 28, 48.1f, 29.8f, 46.1f,
			32.1f, 44.7f, 34.7f, 44.3f, 37.4f, 44.7f, 39.7f, 46.1f, 41.5f, 48.1f, 42.6f, 50.6f, 43, 53.5f, 42.5f, 56.4f,
			41.4f, 58.9f, 39.6f, 60.9f, 37.3f, 62.2f, 34.7f, 62.7f
		},
		{
			62.7f, 62.2f, 60.4f, 60.9f, 58.6f, 58.9f, 57.4f, 56.4f, 57, 53.5f, 57.4f, 50.6f, 58.6f, 48.1f, 60.4f, 46.1f,
			62.6f, 44.7f, 65.3f, 44.3f, 67.9f, 44.7f, 70.2f, 46.1f, 72, 48.1f, 73.2f, 50.6f, 73.6f, 53.5f, 73.1f, 56.4f,
			71.9f, 58.9f, 70.2f, 60.9f, 67.9f, 62.2f, 65.3f, 62.7f
		}
	};
}
