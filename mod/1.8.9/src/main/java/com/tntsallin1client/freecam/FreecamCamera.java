package com.tntsallin1client.freecam;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * What the game's camera follows while freecam is active: a player body of our own that exists only
 * on this client - the server never hears of it. The game can point its camera at any entity
 * (spectating works that way), and everything that depends on where the camera is - which chunks
 * are drawn, fog, the view itself - follows from that by itself.
 *
 * <p>A body like in the other versions of the mod (own user request: the same everywhere): in third
 * person (F5) it shows with the player's own skin, without a cape. It is in the client's world so
 * the game draws it, and the world ticks it - that tick is nothing but its movement by the movement
 * keys, held back by walls (see {@link FreecamCollision}).
 */
final class FreecamCamera extends AbstractClientPlayerEntity {
	/** Negative, so it can never be the id of an entity the server sends (those count up from 1). */
	static final int ENTITY_ID = -7326;
	private static final double SPRINT_MULTIPLIER = 3.0;
	private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180.0F;

	FreecamCamera(World world) {
		super(world, new GameProfile(UUID.randomUUID(), "Freecam"));
		this.abilities.flying = true;
	}

	/**
	 * The skin - despite the name this name table gives the method (see
	 * `AbstractClientPlayerEntityMixin`). This body is nobody the server knows, so it would get a
	 * default skin; it wears the player's own instead, whatever that one's comes from.
	 */
	@Override
	public Identifier getCapeId() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player != null ? player.getCapeId() : super.getCapeId();
	}

	/**
	 * The cape, again despite the name - none: it hangs by how the body moved over the last ticks,
	 * which this one, flying about, has no sensible values for.
	 */
	@Override
	public Identifier getSkinId() {
		return null;
	}

	@Override
	public boolean canRenderCapeTexture() {
		return false;
	}

	/** Wide or slim arms - they have to match the skin. */
	@Override
	public String getModel() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player != null ? player.getModel() : super.getModel();
	}

	/** Hat, jacket, sleeves, trouser legs: as the player has them set, not all hidden as on a new entity. */
	@Override
	public boolean isPartVisible(PlayerModelPart part) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player != null ? player.isPartVisible(part) : super.isPartVisible(part);
	}

	@Override
	public boolean isSpectator() {
		return false;
	}

	/** Nothing in the world shoves the camera around. */
	@Override
	public boolean isPushable() {
		return false;
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
		faceWhereItLooks();
	}

	/** The game draws an entity between where it was a tick ago and where it is now. */
	private void rememberPosition() {
		this.prevX = this.x;
		this.prevY = this.y;
		this.prevZ = this.z;
		this.prevTickX = this.x;
		this.prevTickY = this.y;
		this.prevTickZ = this.z;
	}

	/**
	 * Head and body turn with the view at once. The game lets them catch up over several ticks in the
	 * living entity's own tick, which this body never runs - left alone they would stay where they
	 * were while the view turns.
	 */
	private void faceWhereItLooks() {
		this.headYaw = this.yaw;
		this.bodyYaw = this.yaw;
		this.prevHeadYaw = this.prevYaw;
		this.prevBodyYaw = this.prevYaw;
	}

	/** The mouse, already turned into degrees by the game. */
	void turn(float yawChange, float pitchChange) {
		increaseTransforms(yawChange, pitchChange);
		faceWhereItLooks();
	}

	/**
	 * One game tick - nothing of a player's own tick (hunger, drowning, falling, sounds), only the
	 * movement by the keys.
	 */
	@Override
	public void tick() {
		rememberPosition();
		faceWhereItLooks();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
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
