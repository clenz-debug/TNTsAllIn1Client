package com.tntsallin1client.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Puts a feature's settings back to what they are after the first start: its options, its colors
 * and where the HUD editor put it. Whether the feature is switched on stays as it is (own user
 * decision) - someone resetting a feature from its options screen is using it.
 *
 * <p>A feature's settings are the config fields named after it: those starting with one of its
 * prefixes, followed by a new word. That keeps what a feature stores apart from what it is set to -
 * {@code waypoint} covers {@code waypointShowBeam} but not the waypoints themselves
 * ({@code waypointsByWorld}). Key bindings are not part of the config - they belong to the game's
 * controls.
 */
public final class ConfigReset {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");

	/** Every feature with settings beyond its on/off switch. `labelKey` is its name in the mod menu. */
	public enum Feature {
		COORDINATES_HUD("gui.tntsallin1client.menu.coordinates_hud", "coordinatesHudEnabled", "coordinatesHud"),
		ITEM_COUNTER("gui.tntsallin1client.menu.item_counter", "itemCounterEnabled", "itemCounter"),
		FPS_COUNTER("gui.tntsallin1client.menu.fps_counter", "fpsCounterEnabled", "fpsCounter"),
		LATENCY("gui.tntsallin1client.menu.latency_hud", "latencyHudEnabled", "latency"),
		CLOCK("gui.tntsallin1client.menu.clock_hud", "clockHudEnabled", "clock"),
		KEYSTROKES("gui.tntsallin1client.menu.keystrokes", "keystrokesEnabled", "keystrokes"),
		ARMOR_STATUS("gui.tntsallin1client.menu.armor_status", "armorStatusEnabled", "armorStatus"),
		F3_QUICK_INFO("gui.tntsallin1client.menu.f3_quick_info", "f3QuickInfoEnabled", "f3QuickInfo", "systemInfo"),
		ZOOM("gui.tntsallin1client.menu.zoom", "zoomEnabled", "zoom"),
		FREECAM("gui.tntsallin1client.menu.freecam", "freecamEnabled", "freecam"),
		CROSSHAIR("gui.tntsallin1client.menu.crosshair", "customCrosshairEnabled", "customCrosshair", "crosshair"),
		NO_FOG("gui.tntsallin1client.menu.no_fog", "noFogEnabled", "noFog"),
		SPAWN_OVERLAY("gui.tntsallin1client.menu.spawn_overlay", "spawnOverlayEnabled", "spawnOverlay"),
		HITBOX_COLOR("gui.tntsallin1client.menu.hitbox_color", "customHitboxColorEnabled", "customHitbox"),
		BLOCK_OUTLINE_COLOR("gui.tntsallin1client.menu.block_outline_color", "customBlockOutlineColorEnabled", "customBlockOutline"),
		WAYPOINTS("gui.tntsallin1client.menu.waypoints", "waypointsEnabled", "waypoint"),
		QUICK_SORT("gui.tntsallin1client.menu.quick_sort", "quickSortEnabled", "quickSort"),
		DISCORD_PRESENCE("gui.tntsallin1client.menu.discord_presence", "discordPresenceEnabled", "discordPresence");

		public final String labelKey;
		private final String switchField;
		private final String[] prefixes;

		Feature(String labelKey, String switchField, String... prefixes) {
			this.labelKey = labelKey;
			this.switchField = switchField;
			this.prefixes = prefixes;
		}

		/**
		 * Whether the feature puts something on the HUD that the HUD editor can move - told by a
		 * position among its settings (a field with "HudLayout" in its name).
		 */
		public boolean hasHudElement() {
			for (Field field : ClientConfig.class.getFields()) {
				if (field.getName().contains("HudLayout") && owns(field.getName())) {
					return true;
				}
			}
			return false;
		}

		private boolean owns(String fieldName) {
			if (fieldName.equals(this.switchField)) {
				return false;
			}
			for (String prefix : this.prefixes) {
				if (fieldName.startsWith(prefix)
						&& (fieldName.length() == prefix.length() || !Character.isLowerCase(fieldName.charAt(prefix.length())))) {
					return true;
				}
			}
			return false;
		}
	}

	private ConfigReset() {
	}

	/** Resets one feature and saves. */
	public static void reset(Feature feature) {
		ClientConfig config = ClientConfig.get();
		apply(config, feature);
		config.save();
	}

	/** Resets every feature and saves. */
	public static void resetAll() {
		ClientConfig config = ClientConfig.get();
		for (Feature feature : Feature.values()) {
			apply(config, feature);
		}
		config.save();
	}

	/** Copies the feature's fields over from a config that was never changed. */
	private static void apply(ClientConfig config, Feature feature) {
		ClientConfig defaults = new ClientConfig();
		for (Field field : ClientConfig.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) || !feature.owns(field.getName())) {
				continue;
			}
			try {
				field.set(config, field.get(defaults));
			} catch (IllegalAccessException e) {
				LOGGER.warn("Failed to reset the setting " + field.getName() + ".", e);
			}
		}
	}
}
