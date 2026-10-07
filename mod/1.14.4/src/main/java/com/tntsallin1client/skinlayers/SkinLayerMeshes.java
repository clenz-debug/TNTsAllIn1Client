package com.tntsallin1client.skinlayers;

import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.Polygon;
import net.minecraft.client.model.geom.Cube;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The 3D meshes of every skin seen so far, built on first use. A skin's meshes are rebuilt when its
 * picture changes or when the depth setting does.
 *
 * <p>The skin's pixels are read back from the graphics card: that works the same for a downloaded
 * skin and for the default ones, with no need to get at what the game keeps of either. A
 * downloaded skin shows the default skin's picture until it has arrived, in the very same texture -
 * so every skin in use is read again about once a second and its meshes are rebuilt if the picture
 * is another one by then.
 */
final class SkinLayerMeshes {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final int MAX_SKINS = 96;
	/** Skins are 64x64; a resource pack may ship larger default skins, up to this many times as large. */
	private static final int MAX_SCALE = 4;
	private static final long RECHECK_MILLIS = 1000L;
	private static final SkinLayerPart[] PARTS = SkinLayerPart.values();

	/** A box keeps its six sides to itself - found by the field's type, which holds under any name. */
	private static final Field SIDES_FIELD = sidesField();

	private static final Map<Key, Cached> CACHE = new LinkedHashMap<Key, Cached>(64, 0.75F, true);

	private SkinLayerMeshes() {
	}

	private static Field sidesField() {
		for (Field field : Cube.class.getDeclaredFields()) {
			if (field.getType() == Polygon[].class) {
				field.setAccessible(true);
				return field;
			}
		}
		LOGGER.warn("The player model's boxes look different than expected - skin layers stay flat.");
		return null;
	}

	/**
	 * One mesh per {@link SkinLayerPart}, indexed by its ordinal - or null if this skin has to stay
	 * flat. `model` is the player model the skin is drawn on (wide or slim arms). Binds the skin.
	 */
	static SkinLayerMesh[] get(ResourceLocation skin, PlayerModel<AbstractClientPlayer> model) {
		if (SIDES_FIELD == null) {
			return null;
		}
		TextureObject texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
		if (texture == null) {
			return null;
		}
		int depthPercent = ClientConfig.get().skinLayers3dDepthPercent;
		long now = System.currentTimeMillis();
		Key key = new Key(skin, model);
		Cached entry = CACHE.get(key);
		if (entry != null && entry.texture == texture && entry.depthPercent == depthPercent && now - entry.checkedAt < RECHECK_MILLIS) {
			return entry.meshes;
		}

		Image image = read(texture);
		int imageHash = image == null ? 0 : Arrays.hashCode(image.pixels);
		if (entry != null && entry.texture == texture && entry.depthPercent == depthPercent && entry.imageHash == imageHash) {
			entry.checkedAt = now;
			return entry.meshes;
		}
		if (entry != null) {
			entry.delete();
		}
		entry = new Cached(texture, depthPercent, imageHash, image == null ? null : build(image, model, depthPercent), now);
		CACHE.put(key, entry);
		dropEldest();
		return entry.meshes;
	}

	private static void dropEldest() {
		Iterator<Cached> eldestFirst = CACHE.values().iterator();
		while (CACHE.size() > MAX_SKINS && eldestFirst.hasNext()) {
			eldestFirst.next().delete();
			eldestFirst.remove();
		}
	}

	/** The texture's picture as it is on the graphics card right now; null if it is no skin by its size. */
	private static Image read(TextureObject texture) {
		GlStateManager.bindTexture(texture.getId());
		int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		if (width != height || width < 64 || width % 64 != 0 || width > 64 * MAX_SCALE) {
			return null;
		}
		IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
		int[] pixels = new int[width * height];
		buffer.get(pixels);
		return new Image(pixels, width, height);
	}

	private static SkinLayerMesh[] build(Image image, PlayerModel<AbstractClientPlayer> model, int depthPercent) {
		float depth = Mth.clamp(depthPercent, SkinLayers3d.MIN_DEPTH_PERCENT, SkinLayers3d.MAX_DEPTH_PERCENT) / 100.0F;
		SkinLayerMesh[] meshes = new SkinLayerMesh[PARTS.length];
		try {
			for (SkinLayerPart part : PARTS) {
				ModelPart overlay = part.overlay(model);
				if (overlay.cubes.isEmpty()) {
					return null;
				}
				Cube box = overlay.cubes.get(0);
				Polygon[] sides = (Polygon[]) SIDES_FIELD.get(box);
				meshes[part.ordinal()] = SkinLayerMesh.build(box, sides, image.pixels, image.width, image.height, depth + part.depthOffset);
			}
		} catch (IllegalAccessException e) {
			return null;
		}
		return meshes;
	}

	private static final class Image {
		final int[] pixels;
		final int width;
		final int height;

		Image(int[] pixels, int width, int height) {
			this.pixels = pixels;
			this.width = width;
			this.height = height;
		}
	}

	private static final class Key {
		private final ResourceLocation skin;
		private final PlayerModel<AbstractClientPlayer> model;

		Key(ResourceLocation skin, PlayerModel<AbstractClientPlayer> model) {
			this.skin = skin;
			this.model = model;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Key && ((Key) other).skin.equals(this.skin) && ((Key) other).model == this.model;
		}

		@Override
		public int hashCode() {
			return this.skin.hashCode() * 31 + System.identityHashCode(this.model);
		}
	}

	private static final class Cached {
		final TextureObject texture;
		final int depthPercent;
		final int imageHash;
		/** Null when the skin can't be read - it then keeps its flat layers. */
		final SkinLayerMesh[] meshes;
		long checkedAt;

		Cached(TextureObject texture, int depthPercent, int imageHash, SkinLayerMesh[] meshes, long checkedAt) {
			this.texture = texture;
			this.depthPercent = depthPercent;
			this.imageHash = imageHash;
			this.meshes = meshes;
			this.checkedAt = checkedAt;
		}

		void delete() {
			if (this.meshes != null) {
				for (SkinLayerMesh mesh : this.meshes) {
					mesh.delete();
				}
			}
		}
	}
}
