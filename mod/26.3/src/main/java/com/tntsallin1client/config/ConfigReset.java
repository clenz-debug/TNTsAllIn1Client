package com.tntsallin1client.config;

import com.mojang.logging.LogUtils;
import com.tntsallin1client.menu.ArmorStatusOptionsScreen;
import com.tntsallin1client.menu.BlockOutlineColorOptionsScreen;
import com.tntsallin1client.menu.ClientNameLabelOptionsScreen;
import com.tntsallin1client.menu.ClockOptionsScreen;
import com.tntsallin1client.menu.CoordinatesHudOptionsScreen;
import com.tntsallin1client.menu.CrosshairOptionsScreen;
import com.tntsallin1client.menu.DiscordPresenceOptionsScreen;
import com.tntsallin1client.menu.F3OptionsScreen;
import com.tntsallin1client.menu.FpsCounterOptionsScreen;
import com.tntsallin1client.menu.FreecamOptionsScreen;
import com.tntsallin1client.menu.HitboxColorOptionsScreen;
import com.tntsallin1client.menu.ItemCounterOptionsScreen;
import com.tntsallin1client.menu.KeystrokesOptionsScreen;
import com.tntsallin1client.menu.LatencyOptionsScreen;
import com.tntsallin1client.menu.NoFogOptionsScreen;
import com.tntsallin1client.menu.PinnedRecipeOptionsScreen;
import com.tntsallin1client.menu.QuickSortOptionsScreen;
import com.tntsallin1client.menu.SkinLayers3dOptionsScreen;
import com.tntsallin1client.menu.SpawnOverlayOptionsScreen;
import com.tntsallin1client.menu.WaypointOptionsScreen;
import com.tntsallin1client.menu.ZoomOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Puts a feature's settings back to what they are after the first start: its options, its colors
 * and where the HUD editor put it. Whether the feature is switched on stays as it is (own user
 * decision) - someone resetting a feature from its options screen is using it.
 *
 * <p>A feature's settings are the config fields named after it: those starting with one of its
 * prefixes, followed by a new word. That keeps what a feature stores apart from what it is set to -
 * {@code waypoint} covers {@code waypointShowBeam} but not the waypoints themselves
 * ({@code waypointsByWorld}), {@code pinnedRecipe} not the pinned recipes ({@code pinnedRecipes}).
 * Key bindings are not part of the config - they belong to the game's controls. Features that are a
 * resource pack (3D blocks, dark mode) are not reset here either: their state is the pack list.
 */
public final class ConfigReset {
	private static final Logger LOGGER = LogUtils.getLogger();

	/** Every feature with settings beyond its on/off switch. {@code labelKey} is its name in the mod menu. */
	public enum Feature {
		COORDINATES_HUD(CoordinatesHudOptionsScreen.class, "gui.tntsallin1client.menu.coordinates_hud", "coordinatesHudEnabled", "coordinatesHud"),
		ITEM_COUNTER(ItemCounterOptionsScreen.class, "gui.tntsallin1client.menu.item_counter", "itemCounterEnabled", "itemCounter"),
		FPS_COUNTER(FpsCounterOptionsScreen.class, "gui.tntsallin1client.menu.fps_counter", "fpsCounterEnabled", "fpsCounter"),
		LATENCY(LatencyOptionsScreen.class, "gui.tntsallin1client.menu.latency_hud", "latencyHudEnabled", "latency"),
		CLOCK(ClockOptionsScreen.class, "gui.tntsallin1client.menu.clock_hud", "clockHudEnabled", "clock"),
		CLIENT_NAME_LABEL(ClientNameLabelOptionsScreen.class, "gui.tntsallin1client.menu.client_name_label", "clientNameLabelEnabled", "clientNameLabel"),
		KEYSTROKES(KeystrokesOptionsScreen.class, "gui.tntsallin1client.menu.keystrokes", "keystrokesEnabled", "keystrokes"),
		ARMOR_STATUS(ArmorStatusOptionsScreen.class, "gui.tntsallin1client.menu.armor_status", "armorStatusEnabled", "armorStatus"),
		F3_QUICK_INFO(F3OptionsScreen.class, "gui.tntsallin1client.menu.f3_quick_info", "f3QuickInfoEnabled", "f3QuickInfo", "systemInfo"),
		ZOOM(ZoomOptionsScreen.class, "gui.tntsallin1client.menu.zoom", "zoomEnabled", "zoom"),
		FREECAM(FreecamOptionsScreen.class, "gui.tntsallin1client.menu.freecam", "freecamEnabled", "freecam"),
		CROSSHAIR(CrosshairOptionsScreen.class, "gui.tntsallin1client.menu.crosshair", "customCrosshairEnabled", "customCrosshair", "crosshair"),
		NO_FOG(NoFogOptionsScreen.class, "gui.tntsallin1client.menu.no_fog", "noFogEnabled", "noFog"),
		SPAWN_OVERLAY(SpawnOverlayOptionsScreen.class, "gui.tntsallin1client.menu.spawn_overlay", "spawnOverlayEnabled", "spawnOverlay"),
		HITBOX_COLOR(HitboxColorOptionsScreen.class, "gui.tntsallin1client.menu.hitbox_color", "customHitboxColorEnabled", "customHitbox"),
		BLOCK_OUTLINE_COLOR(BlockOutlineColorOptionsScreen.class, "gui.tntsallin1client.menu.block_outline_color", "customBlockOutlineColorEnabled", "customBlockOutline"),
		WAYPOINTS(WaypointOptionsScreen.class, "gui.tntsallin1client.menu.waypoints", "waypointsEnabled", "waypoint"),
		SKIN_LAYERS_3D(SkinLayers3dOptionsScreen.class, "gui.tntsallin1client.menu.skin_layers_3d", "skinLayers3dEnabled", "skinLayers3d"),
		QUICK_SORT(QuickSortOptionsScreen.class, "gui.tntsallin1client.menu.quick_sort", "quickSortEnabled", "quickSort"),
		PINNED_RECIPE(PinnedRecipeOptionsScreen.class, "gui.tntsallin1client.menu.pinned_recipe", "pinnedRecipeEnabled", "pinnedRecipe"),
		DISCORD_PRESENCE(DiscordPresenceOptionsScreen.class, "gui.tntsallin1client.menu.discord_presence", "discordPresenceEnabled", "discordPresence");

		/** The feature's own options screen - where its "Reset" button goes. */
		public final Class<? extends Screen> optionsScreen;
		public final String labelKey;
		private final String switchField;
		private final String[] prefixes;

		Feature(Class<? extends Screen> optionsScreen, String labelKey, String switchField, String... prefixes) {
			this.optionsScreen = optionsScreen;
			this.labelKey = labelKey;
			this.switchField = switchField;
			this.prefixes = prefixes;
		}

		/** The feature whose options screen this is, or null. */
		public static @Nullable Feature of(Screen screen) {
			for (Feature feature : values()) {
				if (feature.optionsScreen == screen.getClass()) {
					return feature;
				}
			}
			return null;
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
				LOGGER.warn("Failed to reset the setting {}", field.getName(), e);
			}
		}
	}
}
