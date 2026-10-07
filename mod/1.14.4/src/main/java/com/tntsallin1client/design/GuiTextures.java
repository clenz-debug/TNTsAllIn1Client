package com.tntsallin1client.design;

import java.awt.image.BufferedImage;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Pictures the client design paints itself (the font's letters, the logo, the feature symbols) as
 * textures of the game. They are painted with Java's own drawing code into a {@link BufferedImage};
 * this version's game takes its textures in a pixel format of its own, so each is copied over once.
 */
public final class GuiTextures {
	private GuiTextures() {
	}

	/** How many screen pixels a GUI pixel is wide - the pictures are painted for exactly that. */
	static int guiScale() {
		return (int) Minecraft.getInstance().window.getGuiScale();
	}

	/** Hands the picture to the game as a texture. `name` is only a label for the texture's id. */
	public static ResourceLocation register(String name, BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		NativeImage pixels = new NativeImage(width, height, false);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// From alpha-red-green-blue to the game's alpha-blue-green-red.
				int argb = image.getRGB(x, y);
				pixels.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >> 16) | ((argb & 0x000000FF) << 16));
			}
		}
		return Minecraft.getInstance().getTextureManager().register(name, new DynamicTexture(pixels));
	}
}
