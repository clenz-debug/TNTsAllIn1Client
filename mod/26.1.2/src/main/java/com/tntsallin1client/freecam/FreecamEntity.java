package com.tntsallin1client.freecam;

import com.mojang.authlib.GameProfile;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A real, but purely client-side entity the render camera follows while freecam is active,
 * instead of overriding {@code Camera}'s position/rotation after the fact (an earlier version of
 * this feature did that, and it fought Sodium's/vanilla's whole chunk-visibility pipeline - both
 * are built around "the camera follows a real entity registered in the level", the same mechanism
 * riding an entity or spectating already uses, and simply don't work correctly when the camera's
 * position is substituted after their own per-frame bookkeeping already ran. Modeled directly on
 * the established, working community "Freecam" mod's own {@code FreeCamera} class
 * (github.com/MinecraftFreecam/Freecam) - same base class, same {@code addEntity}/{@code
 * setCameraEntity}/{@code removeEntity} lifecycle (see {@link FreecamHandler}).
 *
 * <p>{@code AbstractClientPlayer} is declared abstract but - bytecode-confirmed against this
 * project's own mapped jars - has no actually-unresolved abstract methods left anywhere in its
 * ancestry by the time you reach it; a plain concrete subclass with just this constructor
 * compiles cleanly, no forced overrides needed.
 */
public class FreecamEntity extends AbstractClientPlayer {
	private static final double SPRINT_MULTIPLIER = 3.0;

	public FreecamEntity(ClientLevel level) {
		super(level, new GameProfile(UUID.randomUUID(), "Freecam"));
		setPose(Pose.STANDING);
		getAbilities().flying = true;
	}

	/**
	 * {@code AbstractClientPlayer#getSkin()} normally resolves the skin via {@code getPlayerInfo()}
	 * - a lookup keyed by this entity's own (random, fake) UUID against the client's network player
	 * list - which finds nothing for a locally-created entity and falls back to a default
	 * Steve/Alex skin. Overridden to reuse the real player's already-resolved skin instead, so if
	 * freecam is ever viewed in third person (F5) it shows the user's own skin rather than a
	 * default one.
	 */
	@Override
	public PlayerSkin getSkin() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null ? player.getSkin() : super.getSkin();
	}

	/**
	 * {@code Avatar#isModelPartShown} (bytecode-confirmed: {@code Player}/{@code
	 * AbstractClientPlayer} don't override it) reads a synced entity-data byte
	 * ({@code DATA_PLAYER_MODE_CUSTOMISATION}) that defaults to 0 - i.e. every second-layer skin
	 * part (hat, jacket, sleeves, pant legs - the "3D" overlay look) hidden - for any freshly
	 * constructed entity. The real player's byte only ever gets populated via the normal
	 * client-information/network round trip, which this locally-created entity never goes through.
	 * Delegating to the real player's own value instead, same pattern as {@link #getSkin}.
	 */
	@Override
	public boolean isModelPartShown(PlayerModelPart part) {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null ? player.isModelPartShown(part) : super.isModelPartShown(part);
	}

	/**
	 * Fully self-contained - deliberately not calling {@code super.tick()}, which would run the
	 * whole vanilla {@code Player} tick (hunger, water/drowning effects, fall damage, ...), none
	 * of which applies to this client-only camera rig. {@link #setOldPosAndRot()} replaces the
	 * interpolation bookkeeping {@code super.tick()} would otherwise have handled, so rendering
	 * still smoothly interpolates between ticks the same way any other entity's does.
	 */
	@Override
	public void tick() {
		setOldPosAndRot();

		Minecraft client = Minecraft.getInstance();
		Vec3 wish = computeWishMovement(client);
		if (!wish.equals(Vec3.ZERO)) {
			setPos(position().add(FreecamCollision.resolve(level(), getBoundingBox(), wish)));
		}
	}

	/** Same WASD/Space/Shift/Sprint reading as before, just against this entity's own current rotation instead of a separately tracked yaw/pitch. */
	private Vec3 computeWishMovement(Minecraft client) {
		Options options = client.options;
		double forward = 0;
		double strafe = 0;
		double vertical = 0;
		if (options.keyUp.isDown()) {
			forward += 1;
		}
		if (options.keyDown.isDown()) {
			forward -= 1;
		}
		if (options.keyRight.isDown()) {
			strafe += 1;
		}
		if (options.keyLeft.isDown()) {
			strafe -= 1;
		}
		if (options.keyJump.isDown()) {
			vertical += 1;
		}
		if (options.keyShift.isDown()) {
			vertical -= 1;
		}
		if (forward == 0 && strafe == 0 && vertical == 0) {
			return Vec3.ZERO;
		}

		// Forward/back follow the full look direction (including pitch) - flying "into" the view,
		// same as spectator mode. Strafe stays horizontal regardless of pitch, same as creative flight.
		Vec3 lookDirection = Vec3.directionFromRotation(getXRot(), getYRot());
		Vec3 flatForward = Vec3.directionFromRotation(0, getYRot());
		Vec3 right = new Vec3(-flatForward.z, 0, flatForward.x);

		Vec3 wish = lookDirection.scale(forward).add(right.scale(strafe)).add(new Vec3(0, vertical, 0));
		if (wish.equals(Vec3.ZERO)) {
			return Vec3.ZERO;
		}

		double blocksPerTick = ClientConfig.get().freecamSpeed / 20.0;
		if (options.keySprint.isDown()) {
			blocksPerTick *= SPRINT_MULTIPLIER;
		}
		return wish.normalize().scale(blocksPerTick);
	}
}
