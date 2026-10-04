package com.tntsallin1client.skinlayers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.ModelBox;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.TexturedQuad;
import net.minecraft.client.util.math.TexturePosition;
import org.lwjgl.opengl.GL11;

/**
 * One overlay part of one skin as real geometry: every visible pixel of the layer becomes a small
 * slab standing off the body, with side walls wherever the pixel next to it is empty.
 *
 * <p>Built from the overlay part's own box of the game's player model, so the outer surface shows
 * exactly the same pixels in the same places as the game's flat layer - only further out and with
 * depth. The box's six sides say which rectangle of the skin they show; each side is cut into its
 * pixels here. The same construction as in the Fabric versions; drawn from a display list, as this
 * version's models are.
 */
final class SkinLayerMesh {
	/** Four corners (x, y, z each), the normal, and one texture position for the whole quad. */
	private static final int FLOATS_PER_QUAD = 17;
	/** The game does not draw pixels below 10% alpha - those count as empty here too. */
	private static final int MIN_ALPHA = 26;
	/** Model coordinates are pixels, vertices are blocks. */
	private static final float PIXEL = 1.0F / 16.0F;
	private static final int NO_LIST = -1;

	private final int displayList;

	private SkinLayerMesh(int displayList) {
		this.displayList = displayList;
	}

	/** Draws the part; the skin has to be bound and the body part's position applied. Nothing for a part without pixels. */
	void render() {
		if (this.displayList != NO_LIST) {
			GL11.glCallList(this.displayList);
		}
	}

	/** Gives the display list back. The mesh must not be drawn afterwards. */
	void delete() {
		if (this.displayList != NO_LIST) {
			GlAllocationUtils.deleteSingletonList(this.displayList);
		}
	}

	/**
	 * @param box    the overlay part's box
	 * @param sides  that box's six sides, as the game draws them
	 * @param pixels the skin, one ARGB value per pixel, row by row
	 * @param depth  how far the layer stands off the body, in model pixels - at most one, so a side
	 *               wall never reaches past the first pixel of the side around the corner
	 */
	static SkinLayerMesh build(ModelBox box, TexturedQuad[] sides, int[] pixels, int imageWidth, int imageHeight, float depth) {
		float[] center = {(box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2};
		float[] half = {(box.maxX - box.minX) / 2, (box.maxY - box.minY) / 2, (box.maxZ - box.minZ) / 2};

		List<Face> faces = new ArrayList<Face>();
		for (TexturedQuad side : sides) {
			Face face = Face.of(side, center, half, depth, pixels, imageWidth, imageHeight);
			if (face != null) {
				faces.add(face);
			}
		}

		FloatList quads = new FloatList();
		for (Face face : faces) {
			face.emit(faces, depth, imageWidth, imageHeight, quads);
		}
		return new SkinLayerMesh(compile(quads));
	}

	private static int compile(FloatList quads) {
		if (quads.size == 0) {
			return NO_LIST;
		}
		float[] q = quads.values;
		int list = GlAllocationUtils.genLists(1);
		GL11.glNewList(list, GL11.GL_COMPILE);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_NORMAL);
		for (int i = 0; i < quads.size; i += FLOATS_PER_QUAD) {
			for (int corner = i; corner < i + 12; corner += 3) {
				buffer.vertex(q[corner], q[corner + 1], q[corner + 2]).texture(q[i + 15], q[i + 16]).normal(q[i + 12], q[i + 13], q[i + 14]).next();
			}
		}
		tessellator.draw();
		GL11.glEndList();
		return list;
	}

	/** One side of the overlay box, cut into the skin pixels it shows. */
	private static final class Face {
		/** Corner of the pixel at ({@link #minU}, {@link #minV}), and the step to the next pixel in each texture direction. */
		private final float[] origin;
		private final float[] stepU;
		private final float[] stepV;
		private final float[] normal;
		private final int minU;
		private final int minV;
		private final int width;
		private final int height;
		private final boolean[] solid;

		private Face(float[] origin, float[] stepU, float[] stepV, float[] normal, int minU, int minV, int width, int height, boolean[] solid) {
			this.origin = origin;
			this.stepU = stepU;
			this.stepV = stepV;
			this.normal = normal;
			this.minU = minU;
			this.minV = minV;
			this.width = width;
			this.height = height;
			this.solid = solid;
		}

		static Face of(TexturedQuad side, float[] center, float[] half, float depth, int[] pixels, int imageWidth, int imageHeight) {
			TexturePosition[] vertices = side.positions;
			if (vertices.length != 4) {
				return null;
			}
			float[][] corners = new float[4][];
			float[] us = new float[4];
			float[] vs = new float[4];
			float lowU = Float.MAX_VALUE;
			float lowV = Float.MAX_VALUE;
			float highU = -Float.MAX_VALUE;
			float highV = -Float.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				TexturePosition vertex = vertices[i];
				// Same corner of the box, but our own distance off the body instead of the game's.
				corners[i] = new float[] {
						pushOut((float) vertex.position.x, center[0], half[0], depth),
						pushOut((float) vertex.position.y, center[1], half[1], depth),
						pushOut((float) vertex.position.z, center[2], half[2], depth)};
				us[i] = vertex.u * imageWidth;
				vs[i] = vertex.v * imageHeight;
				lowU = Math.min(lowU, us[i]);
				lowV = Math.min(lowV, vs[i]);
				highU = Math.max(highU, us[i]);
				highV = Math.max(highV, vs[i]);
			}
			int minU = Math.round(lowU);
			int minV = Math.round(lowV);
			int width = Math.round(highU) - minU;
			int height = Math.round(highV) - minV;
			if (width <= 0 || height <= 0) {
				return null;
			}
			float[] origin = cornerAt(corners, us, vs, lowU, lowV);
			float[] endU = cornerAt(corners, us, vs, highU, lowV);
			float[] endV = cornerAt(corners, us, vs, lowU, highV);
			if (origin == null || endU == null || endV == null) {
				return null;
			}
			float[] stepU = scale(subtract(endU, origin), 1.0F / width);
			float[] stepV = scale(subtract(endV, origin), 1.0F / height);

			float[] normal = normalize(cross(stepU, stepV));
			float[] middle = add(origin, add(scale(stepU, width / 2.0F), scale(stepV, height / 2.0F)));
			if (dot(normal, subtract(middle, center)) < 0) {
				normal = scale(normal, -1);
			}

			boolean[] solid = new boolean[width * height];
			for (int row = 0; row < height; row++) {
				for (int column = 0; column < width; column++) {
					int x = minU + column;
					int y = minV + row;
					solid[row * width + column] = x >= 0 && y >= 0 && x < imageWidth && y < imageHeight
							&& (pixels[y * imageWidth + x] >>> 24) >= MIN_ALPHA;
				}
			}
			return new Face(origin, stepU, stepV, normal, minU, minV, width, height, solid);
		}

		private static float pushOut(float coordinate, float center, float half, float depth) {
			return center + (coordinate > center ? 1 : -1) * (half + depth);
		}

		private static float[] cornerAt(float[][] corners, float[] us, float[] vs, float u, float v) {
			for (int i = 0; i < corners.length; i++) {
				if (Math.abs(us[i] - u) < 0.01F && Math.abs(vs[i] - v) < 0.01F) {
					return corners[i];
				}
			}
			return null;
		}

		void emit(List<Face> faces, float depth, int imageWidth, int imageHeight, FloatList out) {
			float[] alongU = normalize(this.stepU);
			float[] alongV = normalize(this.stepV);
			float[] againstU = scale(alongU, -1);
			float[] againstV = scale(alongV, -1);
			for (int row = 0; row < this.height; row++) {
				for (int column = 0; column < this.width; column++) {
					if (!this.solid[row * this.width + column]) {
						continue;
					}
					float[] p00 = add(this.origin, add(scale(this.stepU, column), scale(this.stepV, row)));
					float[] p10 = add(p00, this.stepU);
					float[] p01 = add(p00, this.stepV);
					float[] p11 = add(p10, this.stepV);
					// The middle of the pixel for every corner: the whole slab has that pixel's one color.
					float u = (this.minU + column + 0.5F) / imageWidth;
					float v = (this.minV + row + 0.5F) / imageHeight;

					quad(out, p00, p10, p11, p01, this.normal, u, v);
					if (depth <= 0) {
						continue;
					}
					if (!neighbourSolid(faces, column - 1, row, p00, p01)) {
						wall(out, p00, p01, againstU, depth, u, v);
					}
					if (!neighbourSolid(faces, column + 1, row, p10, p11)) {
						wall(out, p10, p11, alongU, depth, u, v);
					}
					if (!neighbourSolid(faces, column, row - 1, p00, p10)) {
						wall(out, p00, p10, againstV, depth, u, v);
					}
					if (!neighbourSolid(faces, column, row + 1, p01, p11)) {
						wall(out, p01, p11, alongV, depth, u, v);
					}
				}
			}
		}

		/**
		 * Whether the pixel across the edge `from`-`to` is filled. On this face that's a plain lookup;
		 * at the face's border the neighbour is the first pixel of the face around the box's corner -
		 * found by its position, so no table of which face touches which is needed.
		 */
		private boolean neighbourSolid(List<Face> faces, int column, int row, float[] from, float[] to) {
			if (column >= 0 && row >= 0 && column < this.width && row < this.height) {
				return this.solid[row * this.width + column];
			}
			// Just below this face's surface, on the box's edge: that point lies on the other face.
			float[] point = add(scale(add(from, to), 0.5F), scale(this.normal, -0.01F));
			for (Face face : faces) {
				if (face == this) {
					continue;
				}
				float[] relative = subtract(point, face.origin);
				if (Math.abs(dot(relative, face.normal)) > 0.001F) {
					continue;
				}
				float u = dot(relative, face.stepU) / dot(face.stepU, face.stepU);
				float v = dot(relative, face.stepV) / dot(face.stepV, face.stepV);
				if (u < 0 || v < 0 || u >= face.width || v >= face.height) {
					continue;
				}
				return face.solid[(int) v * face.width + (int) u];
			}
			return false;
		}

		/** Side wall under the edge `from`-`to`, from the surface down to the body. */
		private void wall(FloatList out, float[] from, float[] to, float[] facing, float depth, float u, float v) {
			float[] down = scale(this.normal, -depth);
			quad(out, from, to, add(to, down), add(from, down), facing, u, v);
		}

		private static void quad(FloatList out, float[] a, float[] b, float[] c, float[] d, float[] normal, float u, float v) {
			// Counter-clockwise seen from the side the normal points to, like the game's own quads.
			if (dot(cross(subtract(b, a), subtract(d, a)), normal) < 0) {
				float[] swap = b;
				b = d;
				d = swap;
			}
			for (float[] corner : new float[][] {a, b, c, d}) {
				out.add(corner[0] * PIXEL);
				out.add(corner[1] * PIXEL);
				out.add(corner[2] * PIXEL);
			}
			out.add(normal[0]);
			out.add(normal[1]);
			out.add(normal[2]);
			out.add(u);
			out.add(v);
		}

		private static float[] add(float[] a, float[] b) {
			return new float[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
		}

		private static float[] subtract(float[] a, float[] b) {
			return new float[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
		}

		private static float[] scale(float[] a, float factor) {
			return new float[] {a[0] * factor, a[1] * factor, a[2] * factor};
		}

		private static float dot(float[] a, float[] b) {
			return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
		}

		private static float[] cross(float[] a, float[] b) {
			return new float[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
		}

		private static float[] normalize(float[] a) {
			float length = (float) Math.sqrt(dot(a, a));
			return length == 0 ? a : scale(a, 1.0F / length);
		}
	}

	/** A growing list of floats. */
	private static final class FloatList {
		float[] values = new float[1024];
		int size;

		void add(float value) {
			if (this.size == this.values.length) {
				this.values = Arrays.copyOf(this.values, this.size * 2);
			}
			this.values[this.size++] = value;
		}
	}
}
