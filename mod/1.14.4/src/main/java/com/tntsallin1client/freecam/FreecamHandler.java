package com.tntsallin1client.freecam;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;

/**
 * Freecam: a key detaches the camera from the player and lets it fly around on its own, held back
 * by walls ({@link FreecamCollision}). The game's camera follows a {@link FreecamCamera} for as long,
 * which the world moves once per tick.
 *
 * <p>The player stays completely frozen meanwhile - no movement, no turning, nothing sent to the
 * server about either - and attacking, mining, placing, using and dropping items are blocked (see
 * the mixins asking {@link #isActive()}). That is deliberate: the feature is meant to stay usable on
 * servers that don't allow acting from a detached camera.
 */
public final class FreecamHandler {
	private static final int HINT_Y = 4;
	private static final int HINT_COLOR = 0xFFFF55;

	private static FreecamCamera camera;
	private static boolean warningShown;

	private FreecamHandler() {
	}

	public static boolean isActive() {
		return camera != null;
	}

	/** Called every game tick. */
	public static void tick(Minecraft client) {
		while (ModKeyBindings.TOGGLE_FREECAM.consumeClick()) {
			// Only from gameplay - with a screen open the key belongs to that screen.
			if (client.screen == null) {
				if (isActive()) {
					exit(client);
				} else {
					tryEnter(client);
				}
			}
		}

		if (client.level == null) {
			// Left the world or server - the warning may show again on the next one.
			warningShown = false;
		}
		if (camera != null) {
			if (client.player == null || camera.level != client.level) {
				exit(client);
			} else {
				playOutHurt(client.player);
			}
		}
	}

	private static void tryEnter(Minecraft client) {
		LocalPlayer player = client.player;
		if (!ClientConfig.get().freecamEnabled || player == null || client.level == null) {
			return;
		}

		// Starts with its eyes exactly where the player's are, looking the same way.
		FreecamCamera newCamera = new FreecamCamera(client.level);
		newCamera.placeAt(player.x, player.y + player.getEyeHeight() - newCamera.getEyeHeight(), player.z, player.yRot, player.xRot);
		client.level.addPlayer(FreecamCamera.ENTITY_ID, newCamera);
		camera = newCamera;
		client.setCameraEntity(newCamera);

		// A block half mined stays half mined on the server unless it is told otherwise.
		client.gameMode.stopDestroyBlock();

		// The player no longer ticks, so nothing brings its "a tick ago" values up to the current ones
		// again - the game would keep drawing it somewhere between the two, twitching. Made equal once
		// here; the walk animation is stopped the same way.
		player.xo = player.x;
		player.yo = player.y;
		player.zo = player.z;
		player.xOld = player.x;
		player.yOld = player.y;
		player.zOld = player.z;
		player.yRotO = player.yRot;
		player.xRotO = player.xRot;
		player.yHeadRotO = player.yHeadRot;
		player.yBodyRotO = player.yBodyRot;
		player.oAttackAnim = player.attackAnim;
		player.animationSpeedOld = 0.0F;
		player.animationSpeed = 0.0F;
		// The cape is drawn from "a tick ago" and current values as well: where it hangs, how far the
		// player has walked and how much it bobs. Left unequal, the cape would twitch on the frozen body.
		player.xCloakO = player.xCloak;
		player.yCloakO = player.yCloak;
		player.zCloakO = player.zCloak;
		player.walkDistO = player.walkDist;
		player.oBob = player.bob;

		if (!warningShown && !client.hasSingleplayerServer()) {
			warningShown = true;
			client.gui.setOverlayMessage(new TextComponent(I18n.get("gui.tntsallin1client.freecam.warning")), false);
		}
	}

	public static void exit(Minecraft client) {
		if (camera == null) {
			return;
		}
		FreecamCamera old = camera;
		camera = null;
		if (client.player != null) {
			client.setCameraEntity(client.player);
		}
		// Out of the world it was put into - unless that world is gone already.
		if (client.level != null && client.level == old.level) {
			client.level.removeEntity(FreecamCamera.ENTITY_ID);
		}
	}

	/**
	 * A hit taken while frozen still arrives (`hurtTime` set to 10, the walk animation's speed to
	 * 1.5), but both are only ever run down again by the player's own tick, which is skipped. Left
	 * alone the body stays red for good and its legs jerk once per tick. Run down here instead, the
	 * same way the tick does for a body standing still.
	 */
	private static void playOutHurt(LocalPlayer player) {
		if (player.hurtTime > 0) {
			player.hurtTime--;
		}
		player.animationSpeedOld = player.animationSpeed;
		player.animationSpeed -= player.animationSpeed * 0.4F;
		player.animationPosition += player.animationSpeed;
	}

	/**
	 * Mouse movement while active, as the game would hand it to the player: turns the camera instead,
	 * scaled by the freecam's own sensitivity.
	 */
	public static void turn(double yawChange, double pitchChange) {
		if (camera != null) {
			double factor = ClientConfig.get().freecamSensitivityPercent / 100.0;
			camera.turn(yawChange * factor, pitchChange * factor);
		}
	}

	/**
	 * A reminder at the top of the screen while active - frozen and unable to move, attack or
	 * interact, it would otherwise be easy to forget that freecam is the reason.
	 */
	public static void renderHint(Minecraft client, int screenWidth) {
		if (camera != null) {
			String hint = I18n.get("gui.tntsallin1client.freecam.hud_active");
			client.font.drawShadow(hint, (screenWidth - client.font.width(hint)) / 2, HINT_Y, HINT_COLOR);
		}
	}
}
