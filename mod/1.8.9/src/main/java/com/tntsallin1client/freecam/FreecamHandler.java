package com.tntsallin1client.freecam;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;

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

	/** Whether the entity is the freecam's own body - a player in the world that is nobody. */
	public static boolean isCamera(Entity entity) {
		return entity instanceof FreecamCamera;
	}

	/** Called every game tick. */
	public static void tick(MinecraftClient client) {
		while (ModKeyBindings.TOGGLE_FREECAM.wasPressed()) {
			// Only from gameplay - with a screen open the key belongs to that screen.
			if (client.currentScreen == null) {
				if (isActive()) {
					exit(client);
				} else {
					tryEnter(client);
				}
			}
		}

		if (client.world == null) {
			// Left the world or server - the warning may show again on the next one.
			warningShown = false;
		}
		if (camera != null) {
			if (client.player == null || camera.world != client.world) {
				exit(client);
			} else {
				playOutHurt(client.player);
			}
		}
	}

	/**
	 * A hit taken while frozen still arrives (`hurtTime` set to 10, the limb swing amount to 1.5),
	 * but both are only ever run down again by the player's own tick, which is skipped. Left alone
	 * the body stays red for good and its legs jerk once per tick. Run down here instead, the same
	 * way the tick does for a body standing still.
	 */
	private static void playOutHurt(ClientPlayerEntity player) {
		if (player.hurtTime > 0) {
			player.hurtTime--;
		}
		player.field_6748 = player.field_6749;
		player.field_6749 -= player.field_6749 * 0.4F;
		player.field_6750 += player.field_6749;
	}

	private static void tryEnter(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (!ClientConfig.get().freecamEnabled || player == null || client.world == null) {
			return;
		}

		// Starts with its eyes exactly where the player's are, looking the same way.
		FreecamCamera newCamera = new FreecamCamera(client.world);
		newCamera.placeAt(player.x, player.y + player.getEyeHeight() - newCamera.getEyeHeight(), player.z, player.yaw, player.pitch);
		client.world.addEntity(FreecamCamera.ENTITY_ID, newCamera);
		camera = newCamera;
		client.setCameraEntity(newCamera);

		// A block half mined stays half mined on the server unless it is told otherwise.
		client.interactionManager.cancelBlockBreaking();

		// The player no longer ticks, so nothing brings its "a tick ago" values up to the current ones
		// again - the game would keep drawing it somewhere between the two, twitching. Made equal once
		// here; the walk animation is stopped the same way (previous and current limb swing amount).
		player.prevX = player.x;
		player.prevY = player.y;
		player.prevZ = player.z;
		player.prevTickX = player.x;
		player.prevTickY = player.y;
		player.prevTickZ = player.z;
		player.prevYaw = player.yaw;
		player.prevPitch = player.pitch;
		player.prevHeadYaw = player.headYaw;
		player.prevBodyYaw = player.bodyYaw;
		player.lastHandSwingProgress = player.handSwingProgress;
		player.field_6748 = 0.0F;
		player.field_6749 = 0.0F;
		// The cape is drawn from "a tick ago" and current values as well: where it hangs, how far the
		// player has walked and how much it bobs. Left unequal, the cape would twitch on the frozen body.
		player.prevCapeX = player.capeX;
		player.prevCapeY = player.capeY;
		player.prevCapeZ = player.capeZ;
		player.prevHorizontalSpeed = player.horizontalSpeed;
		player.prevStrideDistance = player.strideDistance;

		if (!warningShown && !client.isIntegratedServerRunning()) {
			warningShown = true;
			client.inGameHud.setOverlayMessage(I18n.translate("gui.tntsallin1client.freecam.warning"), false);
		}
	}

	public static void exit(MinecraftClient client) {
		if (camera == null) {
			return;
		}
		FreecamCamera old = camera;
		camera = null;
		if (client.player != null) {
			client.setCameraEntity(client.player);
		}
		// Out of the world it was put into - unless that world is gone already.
		if (client.world != null && client.world == old.world) {
			client.world.removeEntity(FreecamCamera.ENTITY_ID);
		}
	}

	/**
	 * Mouse movement while active, already turned into degrees by the game (see `GameRendererMixin`):
	 * turns the camera instead of the player, scaled by the freecam's own sensitivity.
	 */
	public static void turn(float yawChange, float pitchChange) {
		if (camera != null) {
			float factor = ClientConfig.get().freecamSensitivityPercent / 100.0F;
			camera.turn(yawChange * factor, pitchChange * factor);
		}
	}

	/**
	 * A reminder at the top of the screen while active - frozen and unable to move, attack or
	 * interact, it would otherwise be easy to forget that freecam is the reason.
	 */
	public static void renderHint(MinecraftClient client, int screenWidth) {
		if (camera != null) {
			String hint = I18n.translate("gui.tntsallin1client.freecam.hud_active");
			client.textRenderer.drawWithShadow(hint, (screenWidth - client.textRenderer.getStringWidth(hint)) / 2, HINT_Y, HINT_COLOR);
		}
	}
}
