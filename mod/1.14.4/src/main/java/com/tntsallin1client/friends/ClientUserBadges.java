package com.tntsallin1client.friends;

import com.tntsallin1client.design.GuiTextures;
import com.tntsallin1client.freecam.FreecamHandler;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

/**
 * The client logo in front of the nametag of every player who uses our client too, drawn in that
 * player's own launcher theme colors - the same logo, and the same exchange with the launcher, as in
 * the other versions of the mod.
 *
 * <p>Only the launcher talks to our backend. Once a second we list the ids of the players around us
 * in {@link #PLAYERS_FILE}; the launcher asks the backend which of them use our client and in which
 * colors, and answers as {@code clientUsers} in {@link #INBOX_FILE} (the file it also hands the
 * Fabric versions their friends data in - this version reads nothing else from it). Without our
 * launcher, or offline, there is no such file, and nobody gets a logo.
 *
 * <p>The logo's parts are white shapes side by side in one image (made by
 * {@code designs/branding/make_client_badge.py}, a copy of the other versions' font texture), drawn
 * on top of each other, each tinted in one theme color. The octagon's fill is left out, like there:
 * the nametag's own background shows through.
 */
public final class ClientUserBadges {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String PLAYERS_FILE = "tntsallin1client-players.json";
	private static final String INBOX_FILE = "tntsallin1client-friends.json";
	private static final int TICKS_BETWEEN_CHECKS = 20;

	private static final String IMAGE = "/assets/tntsallin1client/textures/font/client_badge.png";
	/** The image: one square cell per layer, left to right in {@link #LAYER_COLOR_KEYS} order. */
	private static final int CELL_SIZE = 32;
	private static final String[] LAYER_COLOR_KEYS = {"background1", "background2", "accent1", "accent2", "accent3", "accent4"};
	/** Layer 0 is the octagon's fill, which is left out - drawing starts with its outline. */
	private static final int FIRST_DRAWN_LAYER = 1;
	/** The launcher's default theme colors, for players whose launcher hasn't reported colors yet. */
	private static final int[] DEFAULT_COLORS = {0x000000, 0x1A1A1A, 0x3D3D3D, 0x4D4D4D, 0x5D5D5D, 0x6D6D6D};

	/** Size of the logo on the nametag, in the units the name is drawn in - as tall as a line of text. */
	private static final int LOGO_SIZE = 8;
	private static final int NAME_GAP = 2;
	/** The same see-through black the game puts behind a name. */
	private static final float PLATE_ALPHA = 0.25F;
	/** The game draws a name faintly through walls and while sneaking, with this share of full opacity. */
	private static final float FAINT_ALPHA = 0x20 / 255.0F;

	private static int tickCounter;
	private static long inboxModified = -1;
	private static String lastWritten;
	/** Undashed player id -> that player's logo colors (RGB, in LAYER_COLOR_KEYS order). */
	private static Map<String, int[]> clientUsers = Collections.emptyMap();
	private static ResourceLocation texture;
	private static boolean textureFailed;
	/** The player whose name the game is drawing right now - see {@link #beginName}. */
	private static Player nameOwner;

	private ClientUserBadges() {
	}

	/** Called every game tick. */
	public static void tick(Minecraft client) {
		if (++tickCounter < TICKS_BETWEEN_CHECKS) {
			return;
		}
		tickCounter = 0;

		File configDir = new File(client.gameDirectory, "config");
		File inbox = new File(configDir, INBOX_FILE);
		// 0 for a file that isn't there.
		long modified = inbox.lastModified();
		if (modified != inboxModified) {
			inboxModified = modified;
			clientUsers = modified == 0 ? Collections.<String, int[]>emptyMap() : readClientUsers(inbox);
		}
		// Not started by our launcher with an online account - nobody would read the list.
		if (modified == 0) {
			return;
		}

		// Sorted, so the same players always give the same file and nothing is rewritten needlessly.
		Set<String> ids = new TreeSet<String>();
		if (client.level != null) {
			for (Player player : client.level.players()) {
				if (!FreecamHandler.isCamera(player)) {
					ids.add(undashed(player.getUUID()));
				}
			}
		}
		JsonArray array = new JsonArray();
		for (String id : ids) {
			array.add(new JsonPrimitive(id));
		}
		JsonObject json = new JsonObject();
		json.add("uuids", array);
		String serialized = new Gson().toJson(json);
		if (serialized.equals(lastWritten)) {
			return;
		}
		try {
			Files.write(new File(configDir, PLAYERS_FILE).toPath(), serialized.getBytes(StandardCharsets.UTF_8));
			lastWritten = serialized;
		} catch (IOException e) {
			LOGGER.warn("Failed to write the player list for the launcher.", e);
		}
	}

	private static Map<String, int[]> readClientUsers(File inbox) {
		Map<String, int[]> users = new HashMap<String, int[]>();
		try {
			String content = new String(Files.readAllBytes(inbox.toPath()), StandardCharsets.UTF_8);
			JsonObject json = new JsonParser().parse(content).getAsJsonObject();
			if (json.has("clientUsers") && json.get("clientUsers").isJsonArray()) {
				for (JsonElement element : json.getAsJsonArray("clientUsers")) {
					JsonObject user = element.getAsJsonObject();
					JsonObject colors = user.has("colors") && user.get("colors").isJsonObject() ? user.getAsJsonObject("colors") : null;
					users.put(user.get("uuid").getAsString(), parseColors(colors));
				}
			}
		} catch (IOException | RuntimeException e) {
			// Caught while the launcher was writing it, or not what we expect - asked again with its next change.
			LOGGER.warn("Failed to read the launcher's client user list.", e);
		}
		return users;
	}

	/** A `colors` object ("#rrggbb" per key) as RGB values; a missing or malformed color falls back to the default theme's. */
	private static int[] parseColors(JsonObject colors) {
		int[] parsed = DEFAULT_COLORS.clone();
		if (colors == null) {
			return parsed;
		}
		for (int layer = 0; layer < LAYER_COLOR_KEYS.length; layer++) {
			JsonElement value = colors.get(LAYER_COLOR_KEYS[layer]);
			if (value != null && value.isJsonPrimitive()) {
				String hex = value.getAsString();
				if (hex.matches("#[0-9a-fA-F]{6}")) {
					parsed[layer] = Integer.parseInt(hex.substring(1), 16);
				}
			}
		}
		return parsed;
	}

	private static String undashed(UUID id) {
		return id.toString().replace("-", "");
	}

	/**
	 * The game is about to draw this entity's name (not the scoreboard line below a player's name,
	 * which goes another way) - the general routine that then draws the text doesn't know whose it is.
	 */
	public static void beginName(Object entity) {
		nameOwner = entity instanceof Player ? (Player) entity : null;
	}

	public static void endName() {
		nameOwner = null;
	}

	/** Called by the routine that draws a text above an entity, while its position and scale are still in place. */
	public static void drawBesideCurrentName(int nameWidth, int top, boolean sneaking) {
		if (nameOwner != null) {
			drawBeside(nameOwner, nameWidth, top, sneaking);
		}
	}

	/**
	 * Draws the logo to the left of a name the game has just drawn, if that player uses our client.
	 * Called while the name's own position and scale are still in place: the name is `nameWidth` wide,
	 * centered on 0, with its top at `top`.
	 *
	 * @param sneaking the game drew the name the way it does for a sneaking player - faint, and hidden behind walls
	 */
	public static void drawBeside(Player player, int nameWidth, int top, boolean sneaking) {
		int[] colors = clientUsers.get(undashed(player.getUUID()));
		ResourceLocation logo = colors == null ? null : texture();
		if (logo == null) {
			return;
		}
		// The game's plate reaches one unit past the name on every side; ours continues it to the left.
		int plateRight = -(nameWidth / 2) - 1;
		int logoX = plateRight - NAME_GAP - LOGO_SIZE;

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.depthMask(false);
		if (!sneaking) {
			GlStateManager.disableDepthTest();
		}
		GlStateManager.disableTexture();
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(logoX - 1, top - 1, 0.0).color(0.0F, 0.0F, 0.0F, PLATE_ALPHA).endVertex();
		buffer.vertex(logoX - 1, top + 8, 0.0).color(0.0F, 0.0F, 0.0F, PLATE_ALPHA).endVertex();
		buffer.vertex(plateRight, top + 8, 0.0).color(0.0F, 0.0F, 0.0F, PLATE_ALPHA).endVertex();
		buffer.vertex(plateRight, top - 1, 0.0).color(0.0F, 0.0F, 0.0F, PLATE_ALPHA).endVertex();
		tessellator.end();
		GlStateManager.enableTexture();

		Minecraft.getInstance().getTextureManager().bind(logo);
		// Like the name: faintly, visible through walls - unless sneaking, where that is all there is
		// and walls hide it - then once more at full strength for the part that is in view.
		if (sneaking) {
			GlStateManager.depthMask(true);
		}
		drawLayers(colors, logoX, top, FAINT_ALPHA);
		if (!sneaking) {
			GlStateManager.enableDepthTest();
			GlStateManager.depthMask(true);
			drawLayers(colors, logoX, top, 1.0F);
		}

		// What the game's own name drawing leaves behind.
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void drawLayers(int[] colors, int x, int y, float alpha) {
		for (int layer = FIRST_DRAWN_LAYER; layer < colors.length; layer++) {
			int color = colors[layer];
			GlStateManager.color4f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
			GuiComponent.blit(x, y, LOGO_SIZE, LOGO_SIZE, layer * CELL_SIZE, 0.0F, CELL_SIZE, CELL_SIZE,
					LAYER_COLOR_KEYS.length * CELL_SIZE, CELL_SIZE);
		}
	}

	/** The logo image as a texture, loaded from our jar on first use. */
	private static ResourceLocation texture() {
		if (texture == null && !textureFailed) {
			try (InputStream stream = ClientUserBadges.class.getResourceAsStream(IMAGE)) {
				BufferedImage image = ImageIO.read(stream);
				texture = GuiTextures.register("tntsallin1client_badge", image);
				// Drawn at a quarter of its size and smaller - smoothed instead of picking single pixels.
				Minecraft.getInstance().getTextureManager().bind(texture);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			} catch (IOException | RuntimeException e) {
				textureFailed = true;
				LOGGER.warn("Failed to load the client logo for nametags.", e);
			}
		}
		return texture;
	}
}
