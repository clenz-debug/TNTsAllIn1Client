package com.tntsallin1client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Lines, filled quads and camera-facing text in the world - what the newer versions draw through the
 * game's gizmos, which 1.21.1 does not have yet. Meant for Fabric's {@code WorldRenderEvents.LAST}:
 * the camera's rotation is already on the model-view matrix there, so everything is drawn at its
 * position relative to the camera.
 */
public final class WorldShapes {
	private WorldShapes() {
	}

	@FunctionalInterface
	public interface LineSink {
		void line(Vec3 from, Vec3 to, int color);
	}

	/** {@code color} with its alpha multiplied by {@code alpha}. */
	public static int multiplyAlpha(int color, float alpha) {
		return FastColor.ARGB32.color(Math.round(FastColor.ARGB32.alpha(color) * alpha), color);
	}

	/**
	 * Draws every line {@code body} hands over, {@code width} screen pixels wide. {@code onTop} lines
	 * show through blocks.
	 */
	public static void lines(float width, boolean onTop, Consumer<LineSink> body) {
		Vec3 camera = cameraPosition();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		body.accept((from, to, color) -> {
			float x0 = (float) (from.x - camera.x);
			float y0 = (float) (from.y - camera.y);
			float z0 = (float) (from.z - camera.z);
			float x1 = (float) (to.x - camera.x);
			float y1 = (float) (to.y - camera.y);
			float z1 = (float) (to.z - camera.z);
			float dx = x1 - x0;
			float dy = y1 - y0;
			float dz = z1 - z0;
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (length == 0) {
				return;
			}
			dx /= length;
			dy /= length;
			dz /= length;
			buffer.addVertex(x0, y0, z0).setColor(color).setNormal(dx, dy, dz);
			buffer.addVertex(x1, y1, z1).setColor(color).setNormal(dx, dy, dz);
		});
		MeshData mesh = buffer.build();
		if (mesh == null) {
			return;
		}

		// The line shader fades into the world's fog - a far waypoint would vanish in it.
		float fogStart = RenderSystem.getShaderFogStart();
		RenderSystem.setShaderFogStart(Float.MAX_VALUE);
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		RenderSystem.lineWidth(width);
		begin(onTop);
		BufferUploader.drawWithShader(mesh);
		end();
		RenderSystem.lineWidth(1.0F);
		RenderSystem.setShaderFogStart(fogStart);
	}

	public static void line(Vec3 from, Vec3 to, int color, float width, boolean onTop) {
		lines(width, onTop, sink -> sink.line(from, to, color));
	}

	/** The twelve edges of {@code box}. */
	public static void box(AABB box, int color, float width, boolean onTop) {
		lines(width, onTop, sink -> {
			double[] xs = {box.minX, box.maxX};
			double[] ys = {box.minY, box.maxY};
			double[] zs = {box.minZ, box.maxZ};
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					sink.line(new Vec3(box.minX, ys[i], zs[j]), new Vec3(box.maxX, ys[i], zs[j]), color);
					sink.line(new Vec3(xs[i], box.minY, zs[j]), new Vec3(xs[i], box.maxY, zs[j]), color);
					sink.line(new Vec3(xs[i], ys[j], box.minZ), new Vec3(xs[i], ys[j], box.maxZ), color);
				}
			}
		});
	}

	/** A filled quad, visible from both sides. */
	public static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, boolean onTop) {
		Vec3 camera = cameraPosition();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (Vec3 corner : new Vec3[] {a, b, c, d}) {
			buffer.addVertex((float) (corner.x - camera.x), (float) (corner.y - camera.y), (float) (corner.z - camera.z)).setColor(color);
		}
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		begin(onTop);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		end();
	}

	/**
	 * {@code text} facing the camera, centered horizontally with the glyph tops at {@code position}.
	 * One font pixel is {@code scale / 16} blocks, as with the newer versions' text gizmo.
	 */
	public static void text(String text, Vec3 position, int color, float scale, boolean onTop) {
		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		Font font = minecraft.font;
		float pixel = scale / 16.0F;

		PoseStack pose = new PoseStack();
		pose.translate(position.x - cameraPosition.x, position.y - cameraPosition.y, position.z - cameraPosition.z);
		pose.mulPose(camera.rotation());
		pose.scale(pixel, -pixel, pixel);

		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, false, pose.last().pose(), buffers,
				onTop ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		buffers.endBatch();
	}

	private static Vec3 cameraPosition() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
	}

	private static void begin(boolean onTop) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		if (onTop) {
			RenderSystem.disableDepthTest();
		} else {
			RenderSystem.enableDepthTest();
		}
	}

	private static void end() {
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}
}
