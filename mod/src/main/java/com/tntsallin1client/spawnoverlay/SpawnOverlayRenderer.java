package com.tntsallin1client.spawnoverlay;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5j redesigned: was a plain "Light: N" text HUD line; replaced (on
 * user request - a number "didn't feel like physics/danger at a glance") by
 * a key-triggered in-world overlay marking every nearby valid mob-spawn
 * position with a colored "X" - red where a hostile mob can spawn
 * regardless of time, orange where it only gets dark enough at night. Never
 * marks air with nothing solid beneath it, since {@link SpawnRiskCalculator}
 * only ever classifies positions that already passed the real spawn-position
 * checks (valid floor, clear space) - there's no separate "is this air"
 * exclusion needed, it falls out of the same check.
 *
 * <p>Rendered via vanilla's own {@code Gizmos} system (the same one
 * {@code EntityHitboxDebugRendererMixin}/5l already uses) rather than any
 * custom {@code VertexConsumer} code - {@code Gizmos.billboardText(...)} is
 * exactly a colored, camera-facing text label at a world position, which is
 * literally what an "X" mark is. It only works while a {@code GizmoCollector}
 * is active on the render thread, which vanilla itself opens for the
 * <em>entire</em> {@code LevelRenderer#renderLevel} call (see
 * {@code Minecraft.java}) - {@link WorldRenderEvents#BEFORE_DEBUG_RENDER}
 * fires from inside that same call, so calling {@code Gizmos.billboardText}
 * from there works without setting up any collector of our own.
 *
 * <p>Deliberately bounded and throttled: scanning every loaded block would be
 * far too expensive to redo every frame. Re-scans a fixed box around the
 * player ({@link #HORIZONTAL_RADIUS}/{@link #VERTICAL_RADIUS}) every
 * {@link #RESCAN_INTERVAL_TICKS} ticks instead of every frame, and only at
 * all while the overlay is actually visible.
 */
public final class SpawnOverlayRenderer {
	private static final int HORIZONTAL_RADIUS = 10;
	private static final int VERTICAL_RADIUS = 4;
	private static final int RESCAN_INTERVAL_TICKS = 10;
	private static final int ALWAYS_COLOR = 0xFFFF3333;
	private static final int NIGHT_ONLY_COLOR = 0xFFFFAA00;

	private static boolean visible = false;
	private static int ticksUntilRescan = 0;
	private static List<BlockPos> alwaysPositions = List.of();
	private static List<BlockPos> nightOnlyPositions = List.of();

	private SpawnOverlayRenderer() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SpawnOverlayRenderer::tick);
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> draw());
	}

	private static void tick(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		if (!config.spawnOverlayEnabled || client.level == null || client.player == null) {
			setVisible(false);
			return;
		}

		if (config.spawnOverlayHoldMode) {
			setVisible(ModKeyBindings.SPAWN_OVERLAY.isDown());
		} else if (ModKeyBindings.SPAWN_OVERLAY.consumeClick()) {
			setVisible(!visible);
		}

		if (!visible) {
			return;
		}

		if (ticksUntilRescan > 0) {
			ticksUntilRescan--;
			return;
		}
		ticksUntilRescan = RESCAN_INTERVAL_TICKS;
		rescan(client);
	}

	private static void setVisible(boolean newVisible) {
		if (visible && !newVisible) {
			alwaysPositions = List.of();
			nightOnlyPositions = List.of();
		}
		visible = newVisible;
	}

	private static void rescan(Minecraft client) {
		List<BlockPos> newAlways = new ArrayList<>();
		List<BlockPos> newNightOnly = new ArrayList<>();

		BlockPos center = client.player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS, -HORIZONTAL_RADIUS),
				center.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS))) {
			SpawnRisk risk = SpawnRiskCalculator.classify(client.level, pos);
			if (risk == SpawnRisk.ALWAYS) {
				newAlways.add(pos.immutable());
			} else if (risk == SpawnRisk.NIGHT_ONLY) {
				newNightOnly.add(pos.immutable());
			}
		}

		alwaysPositions = newAlways;
		nightOnlyPositions = newNightOnly;
	}

	private static void draw() {
		if (!visible) {
			return;
		}
		for (BlockPos pos : alwaysPositions) {
			drawMark(pos, ALWAYS_COLOR);
		}
		for (BlockPos pos : nightOnlyPositions) {
			drawMark(pos, NIGHT_ONLY_COLOR);
		}
	}

	private static void drawMark(BlockPos pos, int color) {
		Vec3 position = Vec3.atBottomCenterOf(pos).add(0.0, 0.02, 0.0);
		Gizmos.billboardText("X", position, TextGizmo.Style.forColorAndCentered(color).withScale(1.5F));
	}
}
