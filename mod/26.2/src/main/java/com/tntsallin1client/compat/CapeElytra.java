package com.tntsallin1client.compat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A worn elytra takes its look from the player's cape - from a part of the cape's image that the
 * launcher's cape converter only fills with one color, which makes a plain one-colored elytra (own
 * user report). A cape like that leaves the elytra its own look; one whose elytra part is painted
 * is used for it. Asked by `CapeProviderElytraMixin` for every cape image Cape Provider loads.
 */
public final class CapeElytra {
	/** The elytra's part of a cape image, in the 64x32 units the layout is given in. */
	private static final int LAYOUT_WIDTH = 64;
	private static final int ELYTRA_LEFT = 22;
	private static final int ELYTRA_TOP = 0;
	private static final int ELYTRA_WIDTH = 24;
	private static final int ELYTRA_HEIGHT = 22;

	private CapeElytra() {
	}

	/** Whether the elytra part of this cape image (the file as downloaded) is one plain color. Anything that isn't a cape image as we know it is not. */
	public static boolean isPlain(byte[] imageFile) {
		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(imageFile));
		} catch (IOException | RuntimeException e) {
			return false;
		}
		if (image == null) {
			return false;
		}
		int scale = image.getWidth() / LAYOUT_WIDTH;
		int right = Math.min(image.getWidth(), (ELYTRA_LEFT + ELYTRA_WIDTH) * scale);
		int bottom = Math.min(image.getHeight(), (ELYTRA_TOP + ELYTRA_HEIGHT) * scale);
		if (scale < 1 || right <= ELYTRA_LEFT * scale || bottom <= ELYTRA_TOP * scale) {
			return false;
		}
		int first = image.getRGB(ELYTRA_LEFT * scale, ELYTRA_TOP * scale);
		for (int y = ELYTRA_TOP * scale; y < bottom; y++) {
			for (int x = ELYTRA_LEFT * scale; x < right; x++) {
				if (image.getRGB(x, y) != first) {
					return false;
				}
			}
		}
		return true;
	}
}
