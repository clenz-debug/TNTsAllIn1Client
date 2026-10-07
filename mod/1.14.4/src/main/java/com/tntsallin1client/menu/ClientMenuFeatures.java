package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * Every feature the Client Mods menu offers (on/off switch, optional options screen) plus its extra
 * buttons, in menu order - the same order as in the other versions. A feature is listed here once it
 * is brought over to this version; what is not in the list yet does not exist here yet.
 */
final class ClientMenuFeatures {
	private ClientMenuFeatures() {
	}

	/** `screen` is the menu itself: what the options screens lead back to. */
	static void populate(final FeatureSink sink, final Screen screen) {
		final ClientConfig config = ClientConfig.get();

		sink.beginSection("gui.tntsallin1client.menu.section_hud");
		sink.addFeature("gui.tntsallin1client.menu.coordinates_hud", () -> config.coordinatesHudEnabled, value -> config.coordinatesHudEnabled = value,
				() -> new CoordinatesHudOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.item_counter", () -> config.itemCounterEnabled, value -> config.itemCounterEnabled = value,
				() -> new ItemCounterOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.fps_counter", () -> config.fpsCounterEnabled, value -> config.fpsCounterEnabled = value,
				() -> new FpsCounterOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.latency_hud", () -> config.latencyHudEnabled, value -> config.latencyHudEnabled = value,
				() -> new LatencyOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.clock_hud", () -> config.clockHudEnabled, value -> config.clockHudEnabled = value,
				() -> new ClockOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.keystrokes", () -> config.keystrokesEnabled, value -> config.keystrokesEnabled = value,
				() -> new KeystrokesOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.armor_status", () -> config.armorStatusEnabled, value -> config.armorStatusEnabled = value,
				() -> new ArmorStatusOptionsScreen(screen));

		sink.beginSection("gui.tntsallin1client.menu.section_rendering");
		sink.addFeature("gui.tntsallin1client.menu.zoom", () -> config.zoomEnabled, value -> config.zoomEnabled = value,
				() -> new ZoomOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.freecam", () -> config.freecamEnabled, value -> config.freecamEnabled = value,
				() -> new FreecamOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.crosshair", () -> config.customCrosshairEnabled, value -> config.customCrosshairEnabled = value,
				() -> new CrosshairOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.fullbright", () -> config.fullbrightEnabled, value -> config.fullbrightEnabled = value, null);
		sink.addFeature("gui.tntsallin1client.menu.no_fog", () -> config.noFogEnabled, value -> config.noFogEnabled = value,
				() -> new NoFogOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.particle_filter", () -> config.particleFilterEnabled, value -> config.particleFilterEnabled = value,
				() -> new ParticleFilterOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.spawn_overlay", () -> config.spawnOverlayEnabled, value -> config.spawnOverlayEnabled = value,
				() -> new SpawnOverlayOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.hitbox_color", () -> config.customHitboxColorEnabled, value -> config.customHitboxColorEnabled = value,
				() -> new HitboxColorOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.block_outline_color", () -> config.customBlockOutlineColorEnabled, value -> config.customBlockOutlineColorEnabled = value,
				() -> new BlockOutlineColorOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.item_tilt", () -> config.itemTiltEnabled, value -> config.itemTiltEnabled = value, null);
		sink.addFeature("gui.tntsallin1client.menu.waypoints", () -> config.waypointsEnabled, value -> config.waypointsEnabled = value,
				() -> new WaypointOptionsScreen(screen));

		sink.beginSection("gui.tntsallin1client.menu.section_inventory");
		sink.addFeature("gui.tntsallin1client.menu.quick_sort", () -> config.quickSortEnabled, value -> config.quickSortEnabled = value,
				() -> new QuickSortOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.shulker_preview", () -> config.shulkerPreviewEnabled, value -> config.shulkerPreviewEnabled = value,
				() -> new ShulkerPreviewOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.screenshot_toast", () -> config.screenshotToastEnabled, value -> config.screenshotToastEnabled = value, null);
		sink.addFeature("gui.tntsallin1client.menu.pinned_recipe", () -> config.pinnedRecipeEnabled, value -> config.pinnedRecipeEnabled = value,
				() -> new PinnedRecipeOptionsScreen(screen));

		sink.beginSection("gui.tntsallin1client.menu.section_misc");
		sink.addFeature("gui.tntsallin1client.menu.skin_layers_3d", () -> config.skinLayers3dEnabled, value -> config.skinLayers3dEnabled = value,
				() -> new SkinLayers3dOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.discord_presence", () -> config.discordPresenceEnabled, value -> config.discordPresenceEnabled = value,
				() -> new DiscordPresenceOptionsScreen(screen));
		sink.addFeature("gui.tntsallin1client.menu.client_capes", () -> config.clientCapesEnabled, value -> config.clientCapesEnabled = value, null);
		sink.addLink(FeatureSink.LinkRole.HUD_EDITOR, "gui.tntsallin1client.menu.hud_editor_button", () -> new HudEditorScreen(screen));
		// Asks first. Either way the player is back in the menu afterwards.
		sink.addLink(FeatureSink.LinkRole.RESET_ALL, "gui.tntsallin1client.menu.reset_all_button", () -> new ThemedConfirmScreen(confirmed -> {
			if (confirmed) {
				ConfigReset.resetAll();
			}
			Minecraft.getInstance().setScreen(screen);
		}, I18n.get("gui.tntsallin1client.reset.confirm_all_title"),
				I18n.get("gui.tntsallin1client.reset.confirm_all_message")));
	}
}
