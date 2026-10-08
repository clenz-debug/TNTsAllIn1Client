package com.tntsallin1client.skinlayers;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One overlay part of one skin as real geometry: every visible pixel of the layer becomes a small
 * slab standing off the body, with side walls wherever the pixel next to it is empty.
 *
 * <p>Built from the overlay part's own vanilla cube, so the outer surface shows exactly the same
 * pixels in the same places as the flat vanilla layer - only further out and with depth. The cube's
 * six faces say which rectangle of the skin they show; each face is cut into its pixels here.
 */
public final class SkinLayerMesh {
	/** Four corners (x, y, z each), the normal, and one texture position for the whole quad. */
	private static final int FLOATS_PER_QUAD = 17;
	/** Vanilla's entity shader drops pixels below 10% alpha - those count as empty here too. */
	private static final int MIN_ALPHA = 26;
	/** Model coordinates are pixels, vertices are blocks. */
	private static final float PIXEL = 1.0f / 16.0f;

	private final float[] quads;

	private SkinLayerMesh(float[] quads) {
		this.quads = quads;
	}

	public boolean isEmpty() {
		return this.quads.length == 0;
	}

	public void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
		float[] q = this.quads;
		for (int i = 0; i < q.length; i += FLOATS_PER_QUAD) {
			float normalX = q[i + 12];
			float normalY = q[i + 13];
			float normalZ = q[i + 14];
			float u = q[i + 15];
			float v = q[i + 16];
			for (int corner = i; corner < i + 12; corner += 3) {
				consumer.vertex(pose, q[corner], q[corner + 1], q[corner + 2])
						.color(-1)
						.uv(u, v)
						.overlayCoords(overlay)
						.uv2(light)
						.normal(pose, normalX, normalY, normalZ)
						.endVertex();
			}
		}
	}

	/**
	 * @param cube  the overlay part's cube
	 * @param image the skin
	 * @param depth how far the layer stands off the body, in model pixels - at most one, so a side wall
	 *              never reaches past the first pixel of the face around the corner
	 */
	public static SkinLayerMesh build(ModelPart.Cube cube, NativeImage image, float depth) {
		float[] center = {(cube.minX + cube.maxX) / 2, (cube.minY + cube.maxY) / 2, (cube.minZ + cube.maxZ) / 2};
		float[] half = {(cube.maxX - cube.minX) / 2, (cube.maxY - cube.minY) / 2, (cube.maxZ - cube.minZ) / 2};

		List<Face> faces = new ArrayList<>();
		for (ModelPart.Polygon polygon : cube.polygons) {
			Face face = Face.of(polygon, center, half, depth, image);
			if (face != null) {
				faces.add(face);
			}
		}

		FloatArrayList quads = new FloatArrayList();
		for (Face face : faces) {
			face.emit(faces, depth, image, quads);
		}
		return new SkinLayerMesh(quads.toFloatArray());
	}

	/** One side of the overlay cube, cut into the skin pixels it shows. */
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

		static @Nullable Face of(ModelPart.Polygon polygon, float[] center, float[] half, float depth, NativeImage image) {
			ModelPart.Vertex[] vertices = polygon.vertices;
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
				ModelPart.Vertex vertex = vertices[i];
				// Same corner of the cube, but our own distance off the body instead of vanilla's
				corners[i] = new float[] {
						pushOut(vertex.pos.x(), center[0], half[0], depth),
						pushOut(vertex.pos.y(), center[1], half[1], depth),
						pushOut(vertex.pos.z(), center[2], half[2], depth)};
				us[i] = vertex.u * image.getWidth();
				vs[i] = vertex.v * image.getHeight();
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
			float[] stepU = scale(subtract(endU, origin), 1.0f / width);
			float[] stepV = scale(subtract(endV, origin), 1.0f / height);

			float[] normal = normalize(cross(stepU, stepV));
			float[] middle = add(origin, add(scale(stepU, width / 2.0f), scale(stepV, height / 2.0f)));
			if (dot(normal, subtract(middle, center)) < 0) {
				normal = scale(normal, -1);
			}

			boolean[] solid = new boolean[width * height];
			for (int row = 0; row < height; row++) {
				for (int column = 0; column < width; column++) {
					int x = minU + column;
					int y = minV + row;
					solid[row * width + column] = x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()
							&& (image.getPixelRGBA(x, y) >>> 24) >= MIN_ALPHA;
				}
			}
			return new Face(origin, stepU, stepV, normal, minU, minV, width, height, solid);
		}

		private static float pushOut(float coordinate, float center, float half, float depth) {
			return center + (coordinate > center ? 1 : -1) * (half + depth);
		}

		private static float @Nullable [] cornerAt(float[][] corners, float[] us, float[] vs, float u, float v) {
			for (int i = 0; i < corners.length; i++) {
				if (Math.abs(us[i] - u) < 0.01f && Math.abs(vs[i] - v) < 0.01f) {
					return corners[i];
				}
			}
			return null;
		}

		void emit(List<Face> faces, float depth, NativeImage image, FloatArrayList out) {
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
					// The middle of the pixel for every corner: the whole slab has that pixel's one colour
					float u = (this.minU + column + 0.5f) / image.getWidth();
					float v = (this.minV + row + 0.5f) / image.getHeight();

					quad(out, p00, p10, p11, p01, this.normal, u, v);
					if (depth <= 0) {
						continue;
					}
					if (!this.neighbourSolid(faces, column - 1, row, p00, p01)) {
						this.wall(out, p00, p01, againstU, depth, u, v);
					}
					if (!this.neighbourSolid(faces, column + 1, row, p10, p11)) {
						this.wall(out, p10, p11, alongU, depth, u, v);
					}
					if (!this.neighbourSolid(faces, column, row - 1, p00, p10)) {
						this.wall(out, p00, p10, againstV, depth, u, v);
					}
					if (!this.neighbourSolid(faces, column, row + 1, p01, p11)) {
						this.wall(out, p01, p11, alongV, depth, u, v);
					}
				}
			}
		}

		/**
		 * Whether the pixel across the edge {@code from}-{@code to} is filled. On this face that's a plain
		 * lookup; at the face's border the neighbour is the first pixel of the face around the cube's
		 * corner - found by its position, so no table of which face touches which is needed.
		 */
		private boolean neighbourSolid(List<Face> faces, int column, int row, float[] from, float[] to) {
			if (column >= 0 && row >= 0 && column < this.width && row < this.height) {
				return this.solid[row * this.width + column];
			}
			// Just below this face's surface, on the cube's edge: that point lies on the other face
			float[] point = add(scale(add(from, to), 0.5f), scale(this.normal, -0.01f));
			for (Face face : faces) {
				if (face == this) {
					continue;
				}
				float[] relative = subtract(point, face.origin);
				if (Math.abs(dot(relative, face.normal)) > 0.001f) {
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

		/** Side wall under the edge {@code from}-{@code to}, from the surface down to the body. */
		private void wall(FloatArrayList out, float[] from, float[] to, float[] facing, float depth, float u, float v) {
			float[] down = scale(this.normal, -depth);
			quad(out, from, to, add(to, down), add(from, down), facing, u, v);
		}

		private static void quad(FloatArrayList out, float[] a, float[] b, float[] c, float[] d, float[] normal, float u, float v) {
			// Counter-clockwise seen from the side the normal points to, like vanilla's own quads
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
			return length == 0 ? a : scale(a, 1.0f / length);
		}
	}
}
