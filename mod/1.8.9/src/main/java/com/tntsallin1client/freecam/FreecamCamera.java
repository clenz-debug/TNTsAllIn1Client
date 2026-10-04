package com.tntsallin1client.freecam;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * What the game's camera follows while freecam is active: an entity of our own that exists only
 * here - it is never added to the world, so the server never hears of it and nothing in the world
 * reacts to it. The game can point its camera at any entity (spectating works that way), and
 * everything that depends on where the camera is - which chunks are drawn, fog, the view itself -
 * follows from that by itself.
 *
 * <p>A small box with the eye in its middle, moved by the movement keys against the world's
 * collision (see {@link FreecamCollision}).
 */
final class FreecamCamera extends Entity {
	static final float SIZE = 0.6F;
	private static final double SPRINT_MULTIPLIER = 3.0;
	private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180.0F;

	FreecamCamera(World world) {
		super(world);
		setBounds(SIZE, SIZE);
	}

	@Override
	protected void initDataTracker() {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public float getEyeHeight() {
		return SIZE / 2.0F;
	}

	/** Puts the camera somewhere without the game drawing a glide from where it was before. */
	void placeAt(double x, double y, double z, float yaw, float pitch) {
		updatePositionAndAngles(x, y, z, yaw, pitch);
		rememberPosition();
		// Not the angles handed in: the game keeps an entity's own within one turn, while the player's
		// yaw counts every turn it ever made. A "tick ago" value several turns away from the current
		// one would have the view spin through all of them, every tick.
		this.prevYaw = this.yaw;
		this.prevPitch = this.pitch;
	}

	/** The game draws an entity between where it was a tick ago and where it is now - normally the world keeps these up to date. */
	private void rememberPosition() {
		this.prevX = this.x;
		this.prevY = this.y;
		this.prevZ = this.z;
		this.prevTickX = this.x;
		this.prevTickY = this.y;
		this.prevTickZ = this.z;
	}

	/** One game tick of movement. */
	void move(MinecraftClient client) {
		rememberPosition();

		GameOptions options = client.options;
		double forward = (options.forwardKey.isPressed() ? 1 : 0) - (options.backKey.isPressed() ? 1 : 0);
		double strafe = (options.rightKey.isPressed() ? 1 : 0) - (options.leftKey.isPressed() ? 1 : 0);
		double vertical = (options.jumpKey.isPressed() ? 1 : 0) - (options.sneakKey.isPressed() ? 1 : 0);
		if (forward == 0 && strafe == 0 && vertical == 0) {
			return;
		}

		// Forward and back follow the full look direction (including pitch) - flying into the view,
		// like spectator mode. Strafing stays horizontal whatever the pitch, like creative flight.
		float yawRadians = -this.yaw * DEGREES_TO_RADIANS - (float) Math.PI;
		float pitchRadians = -this.pitch * DEGREES_TO_RADIANS;
		double flatX = -MathHelper.sin(yawRadians);
		double flatZ = -MathHelper.cos(yawRadians);
		double horizontal = MathHelper.cos(pitchRadians);
		double moveX = flatX * horizontal * forward - flatZ * strafe;
		double moveY = MathHelper.sin(pitchRadians) * forward + vertical;
		double moveZ = flatZ * horizontal * forward + flatX * strafe;
		double length = Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ);
		if (length < 1.0E-6) {
			return;
		}

		double blocksPerTick = ClientConfig.get().freecamSpeed / 20.0;
		if (options.sprintKey.isPressed()) {
			blocksPerTick *= SPRINT_MULTIPLIER;
		}
		double scale = blocksPerTick / length;
		double[] allowed = FreecamCollision.resolve(client, this, moveX * scale, moveY * scale, moveZ * scale);
		updatePosition(this.x + allowed[0], this.y + allowed[1], this.z + allowed[2]);
	}
}
