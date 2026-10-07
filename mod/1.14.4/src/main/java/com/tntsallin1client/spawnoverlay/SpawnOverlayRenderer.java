package com.tntsallin1client.spawnoverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;

/**
 * The light level overlay: a key marks every place nearby where a hostile mob can spawn with a
 * colored "X" on the ground - red where it can at any time, orange where it is only dark enough at
 * night. The same overlay as in the other versions of the mod; what counts as a place to spawn is
 * {@link SpawnRiskCalculator}'s business.
 *
 * <p>The marks are two lines across the top of the block below, lying flat on it. They are part of
 * the world: hidden behind walls like anything else on the ground.
 *
 * <p>Looking at every block around the player is too much to do every frame. A fixed box around the
 * player is gone through every {@link #RESCAN_INTERVAL_TICKS} ticks, and only while the overlay is
 * showing.
 */
public final class SpawnOverlayRenderer {
	private static final int HORIZONTAL_RADIUS = 10;
	private static final int VERTICAL_RADIUS = 4;
	private static final int RESCAN_INTERVAL_TICKS = 10;
	private static final int ALWAYS_COLOR = 0xFF3333;
	private static final int NIGHT_ONLY_COLOR = 0xFFAA00;
	private static final float LINE_WIDTH = 1.5F;
	/** Lifts the marks off the block's top face, which they would otherwise flicker with. */
	private static final double HEIGHT_ABOVE_GROUND = 0.02;

	private static boolean visible;
	private static boolean keyWasDown;
	private static int ticksUntilRescan;
	private static List<BlockPos> alwaysPositions = Collections.emptyList();
	private static List<BlockPos> nightOnlyPositions = Collections.emptyList();

	private SpawnOverlayRenderer() {
	}

	/** Called every game tick. */
	public static void tick(Minecraft client) {
		ClientConfig config = ClientConfig.get();
		if (!config.spawnOverlayEnabled || client.level == null || client.player == null) {
			setVisible(false);
			return;
		}

		// With a screen open the key belongs to that screen (the game lets go of all keys then).
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
			alwaysPositions = Collections.emptyList();
			nightOnlyPositions = Collections.emptyList();
		}
		if (!visible && newVisible) {
			// Shows right away instead of with the next round.
			ticksUntilRescan = 0;
		}
		visible = newVisible;
	}

	private static void rescan(Minecraft client) {
		List<BlockPos> newAlways = new ArrayList<BlockPos>();
		List<BlockPos> newNightOnly = new ArrayList<BlockPos>();

		int centerX = Mth.floor(client.player.x);
		int centerY = Mth.floor(client.player.y);
		int centerZ = Mth.floor(client.player.z);
		for (int x = centerX - HORIZONTAL_RADIUS; x <= centerX + HORIZONTAL_RADIUS; x++) {
			for (int y = centerY - VERTICAL_RADIUS; y <= centerY + VERTICAL_RADIUS; y++) {
				for (int z = centerZ - HORIZONTAL_RADIUS; z <= centerZ + HORIZONTAL_RADIUS; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					SpawnRisk risk = SpawnRiskCalculator.classify(client.level, pos);
					if (risk == SpawnRisk.ALWAYS) {
						newAlways.add(pos);
					} else if (risk == SpawnRisk.NIGHT_ONLY) {
						newNightOnly.add(pos);
					}
				}
			}
		}

		alwaysPositions = newAlways;
		nightOnlyPositions = newNightOnly;
	}

	/** Called once the world is drawn, with the view it was drawn with still set (see `GameRendererMixin`). */
	public static void render() {
		if (!visible || (alwaysPositions.isEmpty() && nightOnlyPositions.isEmpty())) {
			return;
		}
		// The game draws the world around the camera's position.
		Vec3 origin = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		double originX = origin.x;
		double originY = origin.y;
		double originZ = origin.z;

		GlStateManager.disableLighting();
		GlStateManager.disableTexture();
		GlStateManager.lineWidth(LINE_WIDTH);
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
		for (BlockPos pos : alwaysPositions) {
			mark(buffer, pos, originX, originY, originZ, ALWAYS_COLOR);
		}
		for (BlockPos pos : nightOnlyPositions) {
			mark(buffer, pos, originX, originY, originZ, NIGHT_ONLY_COLOR);
		}
		tessellator.end();

		// Back to what the game has set at this point of drawing the world.
		GlStateManager.lineWidth(1.0F);
		GlStateManager.enableTexture();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Two lines from corner to corner of the block's floor - flat on the ground, so they don't turn with the camera. */
	private static void mark(BufferBuilder buffer, BlockPos pos, double originX, double originY, double originZ, int color) {
		double x = pos.getX() - originX;
		double y = pos.getY() + HEIGHT_ABOVE_GROUND - originY;
		double z = pos.getZ() - originZ;
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		buffer.vertex(x, y, z).color(red, green, blue, 255).endVertex();
		buffer.vertex(x + 1.0, y, z + 1.0).color(red, green, blue, 255).endVertex();
		buffer.vertex(x + 1.0, y, z).color(red, green, blue, 255).endVertex();
		buffer.vertex(x, y, z + 1.0).color(red, green, blue, 255).endVertex();
	}
}
