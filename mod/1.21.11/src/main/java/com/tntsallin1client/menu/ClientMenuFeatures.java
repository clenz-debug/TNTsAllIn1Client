package com.tntsallin1client.menu;

import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.compat.CapeProviderCompat;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.debug.QuickInfoDebugEntry;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import com.tntsallin1client.skinlayers.SkinLayers3d;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;


/**
 * Every feature the Client Mods menu offers (on/off switch, optional options screen) plus its extra
 * buttons, in menu order - fed into a {@link FeatureSink}, so the list view ({@link ClientMenuScreen},
 * Minecraft design) and the card view ({@link ClientModsCardScreen}, client design) always show the
 * exact same features without keeping two copies of this list in sync.
 */
public final class ClientMenuFeatures {
	private ClientMenuFeatures() {
	}

	/** {@code parent} is the menu screen itself - every options screen opened from it returns there. */
	public static void populate(FeatureSink sink, Screen parent) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientConfig config = ClientConfig.get();

		sink.beginSection(Component.translatable("gui.tntsallin1client.menu.section_hud"));

		sink.addToggleRow(config.coordinatesHudEnabled, Component.translatable("gui.tntsallin1client.menu.coordinates_hud"),
				value -> {
					config.coordinatesHudEnabled = value;
					config.save();
				},
				() -> new CoordinatesHudOptionsScreen(parent));

		sink.addToggleRow(config.itemCounterEnabled, Component.translatable("gui.tntsallin1client.menu.item_counter"),
				value -> {
					config.itemCounterEnabled = value;
					config.save();
				},
				() -> new ItemCounterOptionsScreen(parent));

		sink.addToggleRow(config.fpsCounterEnabled, Component.translatable("gui.tntsallin1client.menu.fps_counter"),
				value -> {
					config.fpsCounterEnabled = value;
					config.save();
				},
				() -> new FpsCounterOptionsScreen(parent));

		sink.addToggleRow(config.latencyHudEnabled, Component.translatable("gui.tntsallin1client.menu.latency_hud"),
				value -> {
					config.latencyHudEnabled = value;
					config.save();
				},
				() -> new LatencyOptionsScreen(parent));

		sink.addToggleRow(config.clockHudEnabled, Component.translatable("gui.tntsallin1client.menu.clock_hud"),
				value -> {
					config.clockHudEnabled = value;
					config.save();
				},
				() -> new ClockOptionsScreen(parent));

		sink.addToggleRow(config.keystrokesEnabled, Component.translatable("gui.tntsallin1client.menu.keystrokes"),
				value -> {
					config.keystrokesEnabled = value;
					config.save();
				},
				() -> new KeystrokesOptionsScreen(parent));

		sink.addToggleRow(config.armorStatusEnabled, Component.translatable("gui.tntsallin1client.menu.armor_status"),
				value -> {
					config.armorStatusEnabled = value;
					config.save();
				},
				() -> new ArmorStatusOptionsScreen(parent));

		sink.addToggleRow(config.f3QuickInfoEnabled, Component.translatable("gui.tntsallin1client.menu.f3_quick_info"),
				value -> {
					config.f3QuickInfoEnabled = value;
					config.save();
					QuickInfoDebugEntry.applyVanillaEntryVisibility(minecraft);
				},
				() -> new F3OptionsScreen(parent));

		sink.beginSection(Component.translatable("gui.tntsallin1client.menu.section_rendering"));

		sink.addToggleRow(config.zoomEnabled, Component.translatable("gui.tntsallin1client.menu.zoom"),
				value -> {
					config.zoomEnabled = value;
					config.save();
				},
				() -> new ZoomOptionsScreen(parent));

		sink.addToggleRow(config.freecamEnabled, Component.translatable("gui.tntsallin1client.menu.freecam"),
				value -> {
					config.freecamEnabled = value;
					config.save();
				},
				() -> new FreecamOptionsScreen(parent));

		sink.addToggleRow(config.customCrosshairEnabled, Component.translatable("gui.tntsallin1client.menu.crosshair"),
				value -> {
					config.customCrosshairEnabled = value;
					config.save();
				},
				() -> new CrosshairOptionsScreen(parent));

		sink.addToggleRow(config.fullbrightEnabled, Component.translatable("gui.tntsallin1client.menu.fullbright"),
				value -> {
					config.fullbrightEnabled = value;
					config.save();
				});

		sink.addToggleRow(config.noFogEnabled, Component.translatable("gui.tntsallin1client.menu.no_fog"),
				value -> {
					config.noFogEnabled = value;
					config.save();
				},
				() -> new NoFogOptionsScreen(parent));

		// Sodium's own Video-Einstellungen screen replaces vanilla's entirely and doesn't carry this
		// option over (checked directly against the bundled Sodium jar - no "View Bobbing"/"bobView"
		// string anywhere in it), so with Sodium active there's no menu left to reach it from at all.
		// A plain vanilla OptionInstance<Boolean> otherwise - no validation-bypass trick needed like
		// FullbrightHandler's gamma hack, reads/writes straight through, no separate ClientConfig
		// field either since vanilla's own options.txt already persists it.
		sink.addToggleRow(minecraft.options.bobView().get(), Component.translatable("gui.tntsallin1client.menu.view_bobbing"),
				value -> {
					minecraft.options.bobView().set(value);
					minecraft.options.save();
				});

		sink.addToggleRow(config.spawnOverlayEnabled, Component.translatable("gui.tntsallin1client.menu.spawn_overlay"),
				value -> {
					config.spawnOverlayEnabled = value;
					config.save();
				},
				() -> new SpawnOverlayOptionsScreen(parent));

		sink.addToggleRow(config.customHitboxColorEnabled, Component.translatable("gui.tntsallin1client.menu.hitbox_color"),
				value -> {
					config.customHitboxColorEnabled = value;
					config.save();
				},
				() -> new HitboxColorOptionsScreen(parent));

		sink.addToggleRow(config.customBlockOutlineColorEnabled, Component.translatable("gui.tntsallin1client.menu.block_outline_color"),
				value -> {
					config.customBlockOutlineColorEnabled = value;
					config.save();
				},
				() -> new BlockOutlineColorOptionsScreen(parent));

		sink.addToggleRow(config.itemTiltEnabled, Component.translatable("gui.tntsallin1client.menu.item_tilt"),
				value -> {
					config.itemTiltEnabled = value;
					config.save();
				});

		sink.addToggleRow(config.waypointsEnabled, Component.translatable("gui.tntsallin1client.menu.waypoints"),
				value -> {
					config.waypointsEnabled = value;
					config.save();
				},
				() -> new WaypointOptionsScreen(parent));

		// Continuity's own config option, not its ContinuityFeatureStates API: those states are
		// thread-local and never saved, so toggling them here only reached the client thread (not
		// Sodium's chunk-building threads) and was back on after every restart. Same three steps as
		// Continuity's own config screen: set, save continuity.json, rebuild the chunk meshes.
		sink.addToggleRow(ContinuityConfig.INSTANCE.connectedTextures.get(), Component.translatable("gui.tntsallin1client.menu.connected_textures"),
				value -> {
					ContinuityConfig.INSTANCE.connectedTextures.set(value);
					ContinuityConfig.INSTANCE.save();
					minecraft.levelRenderer.allChanged();
				});

		PackRepository packRepository = minecraft.getResourcePackRepository();
		sink.addToggleRow(Blocks3d.anySelected(packRepository), Component.translatable("gui.tntsallin1client.menu.block_models_3d"),
				value -> {
					Blocks3d.setEnabled(packRepository, value);
					minecraft.options.updateResourcePacks(packRepository);
				},
				() -> new BlockModels3dOptionsScreen(parent));

		// Ours if the launcher bundled it, else the third-party pack it replaces. Switching off removes
		// whichever of the two is on.
		String darkModePackId = BundledResourcePacks.darkModePackId(packRepository);
		boolean darkModeEnabled = packRepository.getSelectedIds().stream().anyMatch(BundledResourcePacks::isDarkModePackId);
		sink.addToggleRow(darkModeEnabled, Component.translatable("gui.tntsallin1client.menu.dark_mode"),
				value -> {
					for (String id : List.copyOf(packRepository.getSelectedIds())) {
						if (BundledResourcePacks.isDarkModePackId(id)) {
							packRepository.removePack(id);
						}
					}
					if (value && darkModePackId != null) {
						packRepository.addPack(darkModePackId);
					}
					minecraft.options.updateResourcePacks(packRepository);
				});

		if (SkinLayers3d.isAvailable()) {
			sink.addToggleRow(config.skinLayers3dEnabled, Component.translatable("gui.tntsallin1client.menu.skin_layers_3d"),
					value -> {
						config.skinLayers3dEnabled = value;
						config.save();
					},
					() -> new SkinLayers3dOptionsScreen(parent));
		}

		sink.beginSection(Component.translatable("gui.tntsallin1client.menu.section_inventory"));

		sink.addToggleRow(config.quickSortEnabled, Component.translatable("gui.tntsallin1client.menu.quick_sort"),
				value -> {
					config.quickSortEnabled = value;
					config.save();
				},
				() -> new QuickSortOptionsScreen(parent));

		sink.addToggleRow(config.shulkerPreviewEnabled, Component.translatable("gui.tntsallin1client.menu.shulker_preview"),
				value -> {
					config.shulkerPreviewEnabled = value;
					config.save();
				},
				() -> new ShulkerPreviewOptionsScreen(parent));

		sink.addToggleRow(config.screenshotToastEnabled, Component.translatable("gui.tntsallin1client.menu.screenshot_toast"),
				value -> {
					config.screenshotToastEnabled = value;
					config.save();
				});

		sink.addToggleRow(config.pinnedRecipeEnabled, Component.translatable("gui.tntsallin1client.menu.pinned_recipe"),
				value -> {
					config.pinnedRecipeEnabled = value;
					config.save();
				},
				() -> new PinnedRecipeOptionsScreen(parent));

		sink.beginSection(Component.translatable("gui.tntsallin1client.menu.section_misc"));

		sink.addToggleRow(config.discordPresenceEnabled, Component.translatable("gui.tntsallin1client.menu.discord_presence"),
				value -> {
					config.discordPresenceEnabled = value;
					config.save();
				},
				() -> new DiscordPresenceOptionsScreen(parent));

		// The capes players set in the launcher, shown by the bundled Cape Provider mod - only there to
		// switch while that mod is installed (see CapeProviderCompat).
		if (CapeProviderCompat.isAvailable()) {
			sink.addToggleRow(CapeProviderCompat.isEnabled(), Component.translatable("gui.tntsallin1client.menu.client_capes"),
					CapeProviderCompat::setEnabled);
		}

		sink.addButtonRow(FeatureSink.ButtonRole.HUD_EDITOR, Component.translatable("gui.tntsallin1client.menu.hud_editor_button"),
				() -> minecraft.setScreen(new HudEditorScreen(parent)));

		sink.addButtonRow(FeatureSink.ButtonRole.RESET_ALL, Component.translatable("gui.tntsallin1client.menu.reset_all_button"),
				() -> minecraft.setScreen(ResetButtons.confirm(minecraft, parent,
						Component.translatable("gui.tntsallin1client.reset.confirm_all_title"),
						Component.translatable("gui.tntsallin1client.reset.confirm_all_message"),
						ConfigReset::resetAll)));

		sink.addButtonRow(FeatureSink.ButtonRole.OTHER, Component.translatable("gui.tntsallin1client.menu.external_mods_button"),
				() -> minecraft.setScreen(new ExternalModsScreen(parent)));

		sink.addButtonRow(FeatureSink.ButtonRole.OTHER, Component.translatable("gui.tntsallin1client.menu.credits_button"),
				() -> minecraft.setScreen(new CreditsScreen(parent)));
	}
}
