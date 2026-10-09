package com.tntsallin1client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.mixin.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * Lines, filled quads and camera-facing text in the world - what 1.21.11 and later draw through the
 * game's gizmos, which 1.21.10 does not have yet. Meant for Fabric's
 * {@code WorldRenderEvents.BEFORE_DEBUG_RENDER}: the camera's rotation is already on the model-view
 * matrix there, so everything is drawn at its position relative to the camera, on the event's own
 * pose. Every call draws at once, so shapes cover each other in the order they are drawn.
 */
public final class WorldShapes {
	private static final RenderPipeline LINES_ON_TOP = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
					.withLocation(ResourceLocation.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "pipeline/lines_on_top"))
					.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
					.withDepthWrite(false)
					.build());
	private static final RenderPipeline QUADS_ON_TOP = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(ResourceLocation.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "pipeline/quads_on_top"))
					.withCull(false)
					.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
					.withDepthWrite(false)
					.build());
	private static final RenderType QUADS_ON_TOP_TYPE = RenderType.create("tntsallin1client_quads_on_top", 1536, QUADS_ON_TOP,
			RenderType.CompositeState.builder().createCompositeState(false));
	/** One render type per line width, with and without depth test. */
	private static final Map<Float, RenderType> LINE_TYPES = new HashMap<>();
	private static final Map<Float, RenderType> LINE_ON_TOP_TYPES = new HashMap<>();

	private WorldShapes() {
	}

	@FunctionalInterface
	public interface LineSink {
		void line(Vec3 from, Vec3 to, int color);
	}

	private static RenderType lineType(float width, boolean onTop) {
		return (onTop ? LINE_ON_TOP_TYPES : LINE_TYPES).computeIfAbsent(width, key -> RenderType.create(
				onTop ? "tntsallin1client_lines_on_top" : "tntsallin1client_lines", 1536,
				onTop ? LINES_ON_TOP : RenderPipelines.LINES,
				RenderType.CompositeState.builder()
						.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(key)))
						.createCompositeState(false)));
	}

	/**
	 * Draws every line {@code body} hands over, {@code width} screen pixels wide. {@code onTop} lines
	 * show through blocks and do not fade into the world's fog.
	 */
	public static void lines(PoseStack.Pose pose, float width, boolean onTop, Consumer<LineSink> body) {
		Vec3 camera = cameraPosition();
		RenderType type = lineType(width, onTop);
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		VertexConsumer buffer = buffers.getBuffer(type);
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
			buffer.addVertex(pose, x0, y0, z0).setColor(color).setNormal(pose, dx, dy, dz);
			buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, dx, dy, dz);
		});
		if (onTop) {
			withoutFog(() -> buffers.endBatch(type));
		} else {
			buffers.endBatch(type);
		}
	}

	public static void line(PoseStack.Pose pose, Vec3 from, Vec3 to, int color, float width, boolean onTop) {
		lines(pose, width, onTop, sink -> sink.line(from, to, color));
	}

	/** The twelve edges of {@code box}. */
	public static void box(PoseStack.Pose pose, AABB box, int color, float width, boolean onTop) {
		lines(pose, width, onTop, sink -> {
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

	/** A filled quad showing through blocks, visible from both sides. */
	public static void quadOnTop(PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		Vec3 camera = cameraPosition();
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		VertexConsumer buffer = buffers.getBuffer(QUADS_ON_TOP_TYPE);
		for (Vec3 corner : new Vec3[] {a, b, c, d}) {
			buffer.addVertex(pose, (float) (corner.x - camera.x), (float) (corner.y - camera.y), (float) (corner.z - camera.z)).setColor(color);
		}
		buffers.endBatch(QUADS_ON_TOP_TYPE);
	}

	/**
	 * {@code text} facing the camera and showing through blocks, centered horizontally with the glyph
	 * tops at {@code position}. One font pixel is {@code scale / 16} blocks, as with the later
	 * versions' text gizmo.
	 */
	public static void textOnTop(PoseStack.Pose pose, String text, Vec3 position, int color, float scale) {
		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.position();
		Font font = minecraft.font;
		float pixel = scale / 16.0F;

		PoseStack stack = new PoseStack();
		stack.last().set(pose);
		stack.translate(position.x - cameraPosition.x, position.y - cameraPosition.y, position.z - cameraPosition.z);
		stack.mulPose(camera.rotation());
		stack.scale(pixel, -pixel, pixel);

		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, false, stack.last().pose(), buffers,
				Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
		buffers.endBatch();
	}

	private static Vec3 cameraPosition() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().position();
	}

	/** The line shader fades into the world's fog - a far waypoint would vanish in it. */
	private static void withoutFog(Runnable draw) {
		GpuBufferSlice fog = RenderSystem.getShaderFog();
		FogRenderer fogRenderer = ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).tntsallin1client$getFogRenderer();
		RenderSystem.setShaderFog(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
		try {
			draw.run();
		} finally {
			RenderSystem.setShaderFog(fog);
		}
	}
}
