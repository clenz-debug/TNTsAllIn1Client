package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.debug.QuickInfoDebugEntry;
import dev.tr7zw.skinlayers.SkinLayersModBase;
import dev.tr7zw.skinlayers.versionless.ModBase;
import me.pepperbell.continuity.api.client.ContinuityFeatureStates;
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
	/**
	 * Exact filenames of the 5p 3D-block-model resource packs bundled in
	 * {@code launcher/resourcepacks-bundle/} (pack IDs are "file/" + the file
	 * name on disk, see vanilla's {@code FolderRepositorySource}) - excludes
	 * the unrelated Default Dark Mode pack. Update this list if any of those
	 * files are ever renamed or upgraded to a new version number, same
	 * upkeep requirement as {@link CreditsScreen}'s own hardcoded entry list.
	 */
	private static final List<String> BLOCK_MODEL_PACK_IDS = List.of(
			"file/Bushy-Vegetation-3.5.2.zip",
			"file/3D-Bushy-Bushie-1.0.zip",
			"file/Mushrooms-Plus-26.1_v1.4.zip",
			"file/Vanilla-Spinning-Stonecutter-3D-1.0.0.zip",
			"file/VanillaTweaks_r346678_MC1.21.x.zip");

	/** Same "file/" + filename scheme as {@link #BLOCK_MODEL_PACK_IDS}, see there - the 5r dark-inventory resource pack. */
	private static final String DARK_MODE_PACK_ID = "file/Default-Dark-Mode-1.21.11-2026.4.0.zip";

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

		sink.addToggleRow(config.clientNameLabelEnabled, Component.translatable("gui.tntsallin1client.menu.client_name_label"),
				value -> {
					config.clientNameLabelEnabled = value;
					config.save();
				},
				() -> new ClientNameLabelOptionsScreen(parent));

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

		ContinuityFeatureStates.FeatureState connectedTextures = ContinuityFeatureStates.get().getConnectedTexturesState();
		sink.addToggleRow(connectedTextures.isEnabled(), Component.translatable("gui.tntsallin1client.menu.connected_textures"),
				value -> {
					if (value) {
						connectedTextures.enable();
					} else {
						connectedTextures.disable();
					}
				});

		ContinuityFeatureStates.FeatureState emissiveTextures = ContinuityFeatureStates.get().getEmissiveTexturesState();
		sink.addToggleRow(emissiveTextures.isEnabled(), Component.translatable("gui.tntsallin1client.menu.emissive_textures"),
				value -> {
					if (value) {
						emissiveTextures.enable();
					} else {
						emissiveTextures.disable();
					}
				});

		PackRepository packRepository = minecraft.getResourcePackRepository();
		boolean blockModels3dEnabled = BLOCK_MODEL_PACK_IDS.stream().anyMatch(packRepository.getSelectedIds()::contains);
		sink.addToggleRow(blockModels3dEnabled, Component.translatable("gui.tntsallin1client.menu.block_models_3d"),
				value -> {
					if (value) {
						BLOCK_MODEL_PACK_IDS.forEach(packRepository::addPack);
					} else {
						BLOCK_MODEL_PACK_IDS.forEach(packRepository::removePack);
					}
					minecraft.options.updateResourcePacks(packRepository);
				});

		boolean darkModeEnabled = packRepository.getSelectedIds().contains(DARK_MODE_PACK_ID);
		sink.addToggleRow(darkModeEnabled, Component.translatable("gui.tntsallin1client.menu.dark_mode"),
				value -> {
					if (value) {
						packRepository.addPack(DARK_MODE_PACK_ID);
					} else {
						packRepository.removePack(DARK_MODE_PACK_ID);
					}
					minecraft.options.updateResourcePacks(packRepository);
				});

		boolean skinLayers3dEnabled = ModBase.config.enableHat || ModBase.config.enableJacket
				|| ModBase.config.enableLeftSleeve || ModBase.config.enableRightSleeve
				|| ModBase.config.enableLeftPants || ModBase.config.enableRightPants;
		sink.addToggleRow(skinLayers3dEnabled, Component.translatable("gui.tntsallin1client.menu.skin_layers_3d"),
				value -> {
					ModBase.config.enableHat = value;
					ModBase.config.enableJacket = value;
					ModBase.config.enableLeftSleeve = value;
					ModBase.config.enableRightSleeve = value;
					ModBase.config.enableLeftPants = value;
					ModBase.config.enableRightPants = value;
					SkinLayersModBase.instance.writeConfig();
				});

		sink.beginSection(Component.translatable("gui.tntsallin1client.menu.section_inventory"));

		sink.addToggleRow(config.quickSortEnabled, Component.translatable("gui.tntsallin1client.menu.quick_sort"),
				value -> {
					config.quickSortEnabled = value;
					config.save();
				},
				() -> new QuickSortOptionsScreen(parent));

		sink.addToggleRow(config.containerClickPacingEnabled, Component.translatable("gui.tntsallin1client.menu.container_click_pacing"),
				value -> {
					config.containerClickPacingEnabled = value;
					config.save();
				});

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

		sink.addButtonRow(FeatureSink.ButtonRole.HUD_EDITOR, Component.translatable("gui.tntsallin1client.menu.hud_editor_button"),
				() -> minecraft.setScreen(new HudEditorScreen(parent)));

		sink.addButtonRow(FeatureSink.ButtonRole.OTHER, Component.translatable("gui.tntsallin1client.menu.external_mods_button"),
				() -> minecraft.setScreen(new ExternalModsScreen(parent)));

		sink.addButtonRow(FeatureSink.ButtonRole.OTHER, Component.translatable("gui.tntsallin1client.menu.credits_button"),
				() -> minecraft.setScreen(new CreditsScreen(parent)));
	}
}
