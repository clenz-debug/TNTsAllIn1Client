package com.tntsallin1client.spawnoverlay;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.render.WorldShapes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
 * <p>Rendered through {@link WorldShapes} from {@link WorldRenderEvents#LAST},
 * all marks in one batch.
 *
 * <p><b>Bugfix (first version drew a billboarded "X" text per mark):</b>
 * billboarded text always rotates to face the camera - fine for a single
 * label, but across a dense field of marks it reads as the whole overlay
 * spinning/tilting as the camera moves, which is exactly what got reported.
 * Switched to {@link #drawMark} drawing two flat
 * line segments across the block's top face instead -
 * real world-space geometry lying on the ground plane, so it stays fixed
 * regardless of viewing angle.
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
	private static boolean keyWasDown = false;
	private static int ticksUntilRescan = 0;
	private static List<BlockPos> alwaysPositions = List.of();
	private static List<BlockPos> nightOnlyPositions = List.of();

	private static final float MARK_WIDTH = 1.5F;

	private SpawnOverlayRenderer() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SpawnOverlayRenderer::tick);
		WorldRenderEvents.LAST.register(context -> draw());
	}

	private static void tick(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		if (!config.spawnOverlayEnabled || client.level == null || client.player == null) {
			setVisible(false);
			return;
		}

		// Toggle reacts to the key's press edge, not consumeClick(): vanilla's
		// KeyboardHandler#keyPress calls KeyMapping.click for GLFW_REPEAT events
		// too, so holding the key a bit too long queued several clicks and
		// flipped the overlay on/off repeatedly - often ending up off.
		boolean keyDown = ModKeyBindings.SPAWN_OVERLAY.isDown();
		if (config.spawnOverlayHoldMode) {
			setVisible(keyDown);
		} else if (keyDown && !keyWasDown) {
			setVisible(!visible);
		}
		keyWasDown = keyDown;

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
		WorldShapes.lines(MARK_WIDTH, false, sink -> {
			for (BlockPos pos : alwaysPositions) {
				drawMark(sink, pos, ALWAYS_COLOR);
			}
			for (BlockPos pos : nightOnlyPositions) {
				drawMark(sink, pos, NIGHT_ONLY_COLOR);
			}
		});
	}

	/**
	 * Drawn as two real world-space line segments across the block's top face
	 * (corner to corner) rather than a billboarded "X" text -
	 * billboarded text always turns to face the camera, which reads as
	 * spinning/tilting when moving through a dense field of marks (reported
	 * after the first version). A flat line pair stays fixed to the ground
	 * plane regardless of camera angle, matching the requested look.
	 */
	private static void drawMark(WorldShapes.LineSink sink, BlockPos pos, int color) {
		double y = pos.getY() + 0.02;
		double x = pos.getX();
		double z = pos.getZ();
		sink.line(new Vec3(x, y, z), new Vec3(x + 1.0, y, z + 1.0), color);
		sink.line(new Vec3(x + 1.0, y, z), new Vec3(x, y, z + 1.0), color);
	}
}
