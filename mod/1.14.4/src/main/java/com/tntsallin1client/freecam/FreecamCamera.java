package com.tntsallin1client.freecam;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;

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
final class FreecamCamera extends AbstractClientPlayer {
	/** Negative, so it can never be the id of an entity the server sends (those count up from 1). */
	static final int ENTITY_ID = -7_326;
	private static final double SPRINT_MULTIPLIER = 3.0;
	private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180.0F;

	FreecamCamera(MultiPlayerLevel level) {
		super(level, new GameProfile(UUID.randomUUID(), "Freecam"));
		// The world keeps it under this number but leaves it to whoever adds an entity to tell the entity.
		setId(ENTITY_ID);
		this.abilities.flying = true;
	}

	/**
	 * Skin and arm width come from the tab list entry of whoever the entity is. This one is nobody
	 * the server knows, so it would get a default skin - it wears the player's own instead.
	 */
	@Override
	protected PlayerInfo getPlayerInfo() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		ClientPacketListener connection = client.getConnection();
		return player == null || connection == null ? null : connection.getPlayerInfo(player.getUUID());
	}

	/** No cape: it hangs by how the body moved over the last ticks, which this one, flying about, has no sensible values for. */
	@Override
	public ResourceLocation getCloakTextureLocation() {
		return null;
	}

	@Override
	public boolean isCapeLoaded() {
		return false;
	}

	@Override
	public ResourceLocation getElytraTextureLocation() {
		return null;
	}

	/** Hat, jacket, sleeves, trouser legs: as the player has them set, not all hidden as on a new entity. */
	@Override
	public boolean isModelPartShown(PlayerModelPart part) {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null ? player.isModelPartShown(part) : super.isModelPartShown(part);
	}

	@Override
	public boolean isSpectator() {
		return false;
	}

	@Override
	public boolean isCreative() {
		return false;
	}

	/** Nothing in the world shoves the camera around. */
	@Override
	public boolean isPushable() {
		return false;
	}

	/** Puts the camera somewhere without the game drawing a glide from where it was before. */
	void placeAt(double x, double y, double z, float yaw, float pitch) {
		absMoveTo(x, y, z, yaw, pitch);
		rememberPosition();
		// Not the angles handed in: the game keeps an entity's own within one turn, while the player's
		// yaw counts every turn it ever made. A "tick ago" value several turns away from the current
		// one would have the view spin through all of them, every tick.
		this.yRotO = this.yRot;
		this.xRotO = this.xRot;
		faceWhereItLooks();
	}

	/** The game draws an entity between where it was a tick ago and where it is now. */
	private void rememberPosition() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		this.xOld = this.x;
		this.yOld = this.y;
		this.zOld = this.z;
	}

	/**
	 * Head and body turn with the view at once. The game lets them catch up over several ticks in the
	 * living entity's own tick, which this body never runs - left alone they would stay where they
	 * were while the view turns.
	 */
	private void faceWhereItLooks() {
		this.yHeadRot = this.yRot;
		this.yBodyRot = this.yRot;
		this.yHeadRotO = this.yRotO;
		this.yBodyRotO = this.yRotO;
	}

	/** The mouse, as the game would hand it to the player. */
	@Override
	public void turn(double yawChange, double pitchChange) {
		super.turn(yawChange, pitchChange);
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
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		Options options = client.options;
		double forward = (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0);
		double strafe = (options.keyRight.isDown() ? 1 : 0) - (options.keyLeft.isDown() ? 1 : 0);
		double vertical = (options.keyJump.isDown() ? 1 : 0) - (options.keySneak.isDown() ? 1 : 0);
		if (forward == 0 && strafe == 0 && vertical == 0) {
			return;
		}

		// Forward and back follow the full look direction (including pitch) - flying into the view,
		// like spectator mode. Strafing stays horizontal whatever the pitch, like creative flight.
		float yawRadians = -this.yRot * DEGREES_TO_RADIANS - (float) Math.PI;
		float pitchRadians = -this.xRot * DEGREES_TO_RADIANS;
		double flatX = -Mth.sin(yawRadians);
		double flatZ = -Mth.cos(yawRadians);
		double horizontal = Mth.cos(pitchRadians);
		double moveX = flatX * horizontal * forward - flatZ * strafe;
		double moveY = Mth.sin(pitchRadians) * forward + vertical;
		double moveZ = flatZ * horizontal * forward + flatX * strafe;
		double length = Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ);
		if (length < 1.0E-6) {
			return;
		}

		double blocksPerTick = ClientConfig.get().freecamSpeed / 20.0;
		if (options.keySprint.isDown()) {
			blocksPerTick *= SPRINT_MULTIPLIER;
		}
		double scale = blocksPerTick / length;
		double[] allowed = FreecamCollision.resolve(client, this, moveX * scale, moveY * scale, moveZ * scale);
		setPos(this.x + allowed[0], this.y + allowed[1], this.z + allowed[2]);
	}
}
