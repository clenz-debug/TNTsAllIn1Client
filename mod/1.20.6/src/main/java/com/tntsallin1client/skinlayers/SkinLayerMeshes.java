package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 3D meshes of every skin seen so far, built on first use. A skin's meshes are rebuilt when its
 * texture is replaced, when the player models are (resource reload) or when the depth setting changes -
 * and all of them whenever a downloaded skin's picture arrives ({@code HttpTextureMixin}): such a
 * texture shows the default skin first and gets its real picture later, staying the same object.
 */
public final class SkinLayerMeshes {
	private static final int MAX_SKINS = 96;
	/** Skins are 64x64; a resource pack may ship larger default skins, up to this many times as large. */
	private static final int MAX_SCALE = 4;
	private static final SkinLayerPart[] PARTS = SkinLayerPart.values();

	private record Key(ResourceLocation skin, PlayerModel<?> model) {
	}

	/** {@code meshes} is null when the skin can't be read - it then keeps its flat vanilla layers. */
	private record Cached(AbstractTexture texture, int depthPercent, SkinLayerMesh @Nullable [] meshes) {
	}

	private static final Map<Key, Cached> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Cached> eldest) {
			return this.size() > MAX_SKINS;
		}
	};

	private SkinLayerMeshes() {
	}

	/**
	 * One mesh per {@link SkinLayerPart}, indexed by its ordinal - or null if this skin has to stay flat.
	 * {@code model} is the player model the skin is drawn on (wide or slim arms).
	 */
	public static SkinLayerMesh @Nullable [] get(ResourceLocation skin, PlayerModel<?> model) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
		int depthPercent = ClientConfig.get().skinLayers3dDepthPercent;
		Key key = new Key(skin, model);
		Cached entry = CACHE.get(key);
		if (entry == null || entry.texture != texture || entry.depthPercent != depthPercent) {
			entry = new Cached(texture, depthPercent, build(texture, model, depthPercent));
			CACHE.put(key, entry);
		}
		return entry.meshes;
	}

	/** Forgets every mesh - they are built again from what the textures hold now. */
	public static void invalidate() {
		CACHE.clear();
	}

	/**
	 * Skin textures keep no copy of their pixels in this version (downloaded ones least of all), so the
	 * picture is read back from the graphics card - the same for every kind of skin.
	 */
	private static SkinLayerMesh @Nullable [] build(AbstractTexture texture, PlayerModel<?> model, int depthPercent) {
		texture.bind();
		int width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		if (width != height || width < 64 || width % 64 != 0 || width > 64 * MAX_SCALE) {
			return null;
		}
		try (NativeImage image = new NativeImage(width, height, false)) {
			image.downloadTexture(0, false);
			return build(image, model, depthPercent);
		}
	}

	private static SkinLayerMesh @Nullable [] build(NativeImage image, PlayerModel<?> model, int depthPercent) {
		float depth = Math.clamp(depthPercent, SkinLayers3d.MIN_DEPTH_PERCENT, SkinLayers3d.MAX_DEPTH_PERCENT) / 100.0f;
		SkinLayerMesh[] meshes = new SkinLayerMesh[PARTS.length];
		for (SkinLayerPart part : PARTS) {
			ModelPart.Cube cube = firstCube(part.overlay(model));
			if (cube == null) {
				return null;
			}
			meshes[part.ordinal()] = SkinLayerMesh.build(cube, image, depth + part.depthOffset);
		}
		return meshes;
	}

	/** {@code ModelPart} keeps its cubes private; walking the part is the public way to reach them. */
	private static ModelPart.@Nullable Cube firstCube(ModelPart part) {
		ModelPart.Cube[] found = new ModelPart.Cube[1];
		part.visit(new PoseStack(), (pose, path, index, cube) -> {
			if (found[0] == null && path.isEmpty()) {
				found[0] = cube;
			}
		});
		return found[0];
	}
}
