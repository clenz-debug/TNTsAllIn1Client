package com.tntsallin1client.freecam;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Lifecycle manager for freecam - the actual movement/collision logic lives on
 * {@link FreecamEntity} itself now (ticked automatically once it's a real entity in the level),
 * this class just creates/attaches/detaches/removes it, forwards mouse rotation to it every
 * frame, and handles the surrounding bookkeeping (multiplayer warning, smartCull safety net -
 * see {@link #tryEnter}).
 *
 * <p>No camera-type override is needed for the real player's body to stay visible: vanilla only
 * hides the entity the camera is currently attached to (see {@code LevelRenderer}'s render-list
 * build, bytecode-verified: {@code entity == camera.entity() && !camera.isDetached()}), and since
 * {@link Minecraft#setCameraEntity} now points at {@link FreecamEntity} instead of the real
 * player, that check never matches the real player anymore, in any camera type. Freecam therefore
 * runs in genuine first-person on the {@link FreecamEntity} by default; pressing F5 during it
 * cycles to third-person like normal and shows the (now correctly non-spinning, real-skinned -
 * see {@link FreecamEntity#getSkin}) fake entity instead, exactly like Vanilla would for any
 * other camera-followed entity.
 *
 * <p>The real player entity stays completely frozen the whole time (see
 * {@link com.tntsallin1client.mixin.LocalPlayerMixin}/{@link com.tntsallin1client.mixin.EntityTurnMixin}) -
 * no movement, no rotation, no physics, no outbound packets - and all attack/mine/place/use
 * interaction is blocked while active ({@link com.tntsallin1client.mixin.MultiPlayerGameModeMixin}),
 * both per explicit user request, since this is meant to be safe to use on servers that don't
 * allow freecam-style cheats. {@link FreecamCollision} keeps the same world-collision (with the
 * doors/fence gates/trapdoors always-passable exception) it always had - only who calls it changed.
 */
public final class FreecamHandler {
	private static final double SENSITIVITY_EXPONENT = 3.0;
	private static final double SENSITIVITY_SCALE = 0.6;
	private static final double SENSITIVITY_OFFSET = 0.2;
	private static final double SENSITIVITY_FACTOR = 8.0;

	private static boolean active;
	private static boolean warningShownThisSession;
	private static boolean previousSmartCull;
	private static FreecamEntity entity;

	private FreecamHandler() {
	}

	public static void tick(Minecraft client) {
		if (ModKeyBindings.TOGGLE_FREECAM.consumeClick()) {
			if (active) {
				exit(client);
			} else {
				tryEnter(client);
			}
		}
	}

	private static void tryEnter(Minecraft client) {
		LocalPlayer player = client.player;
		if (!ClientConfig.get().freecamEnabled || player == null || client.level == null) {
			return;
		}

		entity = new FreecamEntity(client.level);
		entity.snapTo(player.getEyePosition(), player.getYRot(), player.getXRot());
		entity.setYHeadRot(player.getYRot());
		entity.setYBodyRot(player.getYRot());
		entity.setOldPosAndRot();
		// LivingEntity normally catches yBodyRot/yHeadRotO up to yHeadRot gradually every tick via
		// tickHeadTurn() (called from LivingEntity#tick(), which FreecamEntity deliberately never
		// calls - see FreecamEntity's own comment). Left untouched, they'd stay frozen at their
		// spawn defaults forever while rotate() moves yRot/yHeadRot every frame, so the rendered
		// body/head model would keep interpolating from that stale baseline - visible as constant
		// spinning. Syncing all of them here too, not just yRotO/xRotO, so nothing about the model
		// starts out already out of sync.
		entity.yRotO = player.getYRot();
		entity.xRotO = player.getXRot();
		entity.yHeadRotO = player.getYRot();
		entity.yBodyRotO = player.getYRot();
		client.level.addEntity(entity);
		client.setCameraEntity(entity);
		active = true;

		// LocalPlayerMixin/EntityTurnMixin cancel the real player's tick()/turn() outright while
		// freecam is active, so nothing writes to their position/rotation/walk-animation fields
		// again from here on - but whatever they happened to be mid-transition (e.g. still holding
		// a movement key, or mid-stride with a non-zero WalkAnimationState speed) stays frozen
		// as-is forever, including the *old* (pre-interpolation) side of each field. Since
		// LevelRendererMixin makes the real player render normally again despite the camera being
		// elsewhere, the renderer keeps lerping between that stale "old" value and the frozen
		// "current" one every frame using the ordinary (still-advancing) game-time partial tick -
		// most visibly for WalkAnimationState, whose position()/speed() blend by leftover non-zero
		// speed, producing a persistent leg/body twitch if freecam was toggled while moving.
		// Syncing old==current for everything relevant here, once, makes that blend a no-op
		// regardless of partial tick - same fix already applied to FreecamEntity's own rotation
		// above, just for the entity that was already interpolating before freecam even started.
		player.setOldPosAndRot();
		player.yHeadRotO = player.getYHeadRot();
		player.yBodyRotO = player.yBodyRot;
		player.walkAnimation.stop();

		// Kept as a safety net from the earlier camera-override approach - not yet re-verified
		// whether it's still needed now that the camera follows a real entity, but it's harmless
		// to leave in (see the plan this landed with for why removing it wasn't done blindly).
		previousSmartCull = client.smartCull;
		client.smartCull = false;

		maybeWarnMultiplayer(client);
	}

	public static void exit(Minecraft client) {
		// Guards against a no-op call actually restoring anything: the disconnect cleanup hook
		// calls this unconditionally on every disconnect, active or not, and a call with nothing
		// to restore would otherwise stomp the player's real smartCull setting with a stale value.
		if (!active) {
			return;
		}
		active = false;

		if (client.player != null) {
			client.setCameraEntity(client.player);
		}
		if (entity != null && client.level != null) {
			client.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
		}
		entity = null;

		client.smartCull = previousSmartCull;
	}

	public static boolean isActive() {
		return active;
	}

	/**
	 * Called from {@link com.tntsallin1client.mixin.MouseHandlerMixin} with the raw accumulated
	 * mouse delta - applied directly to {@link #entity}'s own rotation, once per frame (matches
	 * vanilla's own turn-rate formula and invertMouseX/Y options, scaled by
	 * {@link ClientConfig#freecamSensitivityPercent} on top). Vanilla's first-person camera
	 * already follows an entity's *current* rotation without any tick-based interpolation - since
	 * that's exactly the code path freecam now goes through too, this gets the same "no jitter"
	 * behavior for free, without needing to reimplement it.
	 */
	public static void rotate(Minecraft client, double dx, double dy) {
		if (!active || entity == null) {
			return;
		}
		double sensitivity = client.options.sensitivity().get();
		double factor = Math.pow(sensitivity * SENSITIVITY_SCALE + SENSITIVITY_OFFSET, SENSITIVITY_EXPONENT) * SENSITIVITY_FACTOR;
		factor *= ClientConfig.get().freecamSensitivityPercent / 100.0;

		double turnX = client.options.invertMouseX().get() ? -dx : dx;
		double turnY = client.options.invertMouseY().get() ? -dy : dy;
		float newYaw = (float) (entity.getYRot() + turnX * factor);
		float newPitch = Mth.clamp((float) (entity.getXRot() + turnY * factor), -90.0f, 90.0f);
		entity.setYRot(newYaw);
		entity.setYHeadRot(newYaw);
		entity.setYBodyRot(newYaw);
		entity.setXRot(newPitch);

		// Camera#setup (view rotation) reads Entity#getViewYRot/getViewXRot, and the player-model
		// renderer (visible body in third person) reads yBodyRot/yHeadRot - both interpolate
		// between their own "O" (previous-frame) field and the current value every frame. Vanilla's
		// own Entity#turn (used for the real player) keeps *O in lockstep with every mouse-delta
		// update for exactly this reason - without it, *O only resyncs once per tick (or, for
		// yBodyRotO, never at all here - see tryEnter()'s comment) while we update the current
		// value at frame rate, so rendering keeps re-interpolating from a stale baseline and
		// visibly wobbles/spins.
		entity.yRotO = newYaw;
		entity.xRotO = newPitch;
		entity.yHeadRotO = newYaw;
		entity.yBodyRotO = newYaw;
	}

	/** Resets the per-session warning flag - called on disconnect so it can show again next server. */
	public static void resetWarning() {
		warningShownThisSession = false;
	}

	private static void maybeWarnMultiplayer(Minecraft client) {
		if (warningShownThisSession) {
			return;
		}
		boolean local = client.isLocalServer() && client.getSingleplayerServer() != null;
		if (local) {
			return;
		}
		warningShownThisSession = true;
		client.player.displayClientMessage(Component.translatable("gui.tntsallin1client.freecam.warning"), true);
	}
}
