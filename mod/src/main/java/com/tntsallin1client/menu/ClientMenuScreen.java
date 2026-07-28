package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.debug.QuickInfoDebugEntry;
import dev.tr7zw.skinlayers.SkinLayersModBase;
import dev.tr7zw.skinlayers.config.ConfigScreenProvider;
import dev.tr7zw.skinlayers.versionless.ModBase;
import me.pepperbell.continuity.api.client.ContinuityFeatureStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Phase 5e: ingame mod menu, top level. Just the on/off switch per feature.
 * Features with more to configure than a toggle get their own dedicated
 * options screen (e.g. {@link MaterialCounterOptionsScreen}), opened via a
 * small button next to that feature's toggle - deliberately not one shared
 * options screen for every feature, which would turn into an unrelated,
 * ever-growing list as more features gain settings. Reachable via the pause
 * menu button ({@link PauseMenuIntegration}) or its own keybind
 * ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 *
 * <p>The row list scrolls (a {@link ContainerObjectSelectionList}, the same
 * base class vanilla's own Controls screen uses for its key bindings) -
 * with 5a-5o's worth of features this no longer fits on screen at every GUI
 * scale, so it needed real scrolling rather than the fixed absolute Y
 * positions this screen used up through 5o.
 */
public class ClientMenuScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 30;

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

	private final @Nullable Screen parent;

	public ClientMenuScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.tntsallin1client.menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		FeatureList list = new FeatureList(this.minecraft, this.width, listHeight, LIST_TOP);

		list.addToggleRow(config.coordinatesHudEnabled, Component.translatable("gui.tntsallin1client.menu.coordinates_hud"),
				value -> {
					config.coordinatesHudEnabled = value;
					config.save();
				},
				() -> new CoordinatesHudOptionsScreen(this));

		list.addToggleRow(config.materialCounterEnabled, Component.translatable("gui.tntsallin1client.menu.material_counter"),
				value -> {
					config.materialCounterEnabled = value;
					config.save();
				},
				() -> new MaterialCounterOptionsScreen(this));

		list.addToggleRow(config.quickSortEnabled, Component.translatable("gui.tntsallin1client.menu.quick_sort"),
				value -> {
					config.quickSortEnabled = value;
					config.save();
				});

		list.addToggleRow(config.f3QuickInfoEnabled, Component.translatable("gui.tntsallin1client.menu.f3_quick_info"),
				value -> {
					config.f3QuickInfoEnabled = value;
					config.save();
					QuickInfoDebugEntry.applyVanillaEntryVisibility(this.minecraft);
				},
				() -> new F3OptionsScreen(this));

		list.addToggleRow(config.clientNameLabelEnabled, Component.translatable("gui.tntsallin1client.menu.client_name_label"),
				value -> {
					config.clientNameLabelEnabled = value;
					config.save();
				},
				() -> new ClientNameLabelOptionsScreen(this));

		list.addToggleRow(config.fpsCounterEnabled, Component.translatable("gui.tntsallin1client.menu.fps_counter"),
				value -> {
					config.fpsCounterEnabled = value;
					config.save();
				},
				() -> new FpsCounterOptionsScreen(this));

		list.addToggleRow(config.zoomEnabled, Component.translatable("gui.tntsallin1client.menu.zoom"),
				value -> {
					config.zoomEnabled = value;
					config.save();
				},
				() -> new ZoomOptionsScreen(this));

		list.addToggleRow(config.customCrosshairEnabled, Component.translatable("gui.tntsallin1client.menu.crosshair"),
				value -> {
					config.customCrosshairEnabled = value;
					config.save();
				},
				() -> new CrosshairOptionsScreen(this));

		list.addToggleRow(config.fullbrightEnabled, Component.translatable("gui.tntsallin1client.menu.fullbright"),
				value -> {
					config.fullbrightEnabled = value;
					config.save();
				});

		list.addToggleRow(config.spawnOverlayEnabled, Component.translatable("gui.tntsallin1client.menu.spawn_overlay"),
				value -> {
					config.spawnOverlayEnabled = value;
					config.save();
				},
				() -> new SpawnOverlayOptionsScreen(this));

		list.addToggleRow(config.shulkerPreviewEnabled, Component.translatable("gui.tntsallin1client.menu.shulker_preview"),
				value -> {
					config.shulkerPreviewEnabled = value;
					config.save();
				},
				() -> new ShulkerPreviewOptionsScreen(this));

		list.addToggleRow(config.customHitboxColorEnabled, Component.translatable("gui.tntsallin1client.menu.hitbox_color"),
				value -> {
					config.customHitboxColorEnabled = value;
					config.save();
				},
				() -> new HitboxColorOptionsScreen(this));

		list.addToggleRow(config.customBlockOutlineColorEnabled, Component.translatable("gui.tntsallin1client.menu.block_outline_color"),
				value -> {
					config.customBlockOutlineColorEnabled = value;
					config.save();
				},
				() -> new BlockOutlineColorOptionsScreen(this));

		list.addToggleRow(config.keystrokesEnabled, Component.translatable("gui.tntsallin1client.menu.keystrokes"),
				value -> {
					config.keystrokesEnabled = value;
					config.save();
				},
				() -> new KeystrokesOptionsScreen(this));

		list.addToggleRow(config.screenshotToastEnabled, Component.translatable("gui.tntsallin1client.menu.screenshot_toast"),
				value -> {
					config.screenshotToastEnabled = value;
					config.save();
				});

		list.addToggleRow(config.itemTiltEnabled, Component.translatable("gui.tntsallin1client.menu.item_tilt"),
				value -> {
					config.itemTiltEnabled = value;
					config.save();
				});

		ContinuityFeatureStates.FeatureState connectedTextures = ContinuityFeatureStates.get().getConnectedTexturesState();
		list.addToggleRow(connectedTextures.isEnabled(), Component.translatable("gui.tntsallin1client.menu.connected_textures"),
				value -> {
					if (value) {
						connectedTextures.enable();
					} else {
						connectedTextures.disable();
					}
				});

		PackRepository packRepository = this.minecraft.getResourcePackRepository();
		boolean blockModels3dEnabled = BLOCK_MODEL_PACK_IDS.stream().anyMatch(packRepository.getSelectedIds()::contains);
		list.addToggleRow(blockModels3dEnabled, Component.translatable("gui.tntsallin1client.menu.block_models_3d"),
				value -> {
					if (value) {
						BLOCK_MODEL_PACK_IDS.forEach(packRepository::addPack);
					} else {
						BLOCK_MODEL_PACK_IDS.forEach(packRepository::removePack);
					}
					this.minecraft.options.updateResourcePacks(packRepository);
				});

		boolean skinLayers3dEnabled = ModBase.config.enableHat || ModBase.config.enableJacket
				|| ModBase.config.enableLeftSleeve || ModBase.config.enableRightSleeve
				|| ModBase.config.enableLeftPants || ModBase.config.enableRightPants;
		list.addToggleRow(skinLayers3dEnabled, Component.translatable("gui.tntsallin1client.menu.skin_layers_3d"),
				value -> {
					ModBase.config.enableHat = value;
					ModBase.config.enableJacket = value;
					ModBase.config.enableLeftSleeve = value;
					ModBase.config.enableRightSleeve = value;
					ModBase.config.enableLeftPants = value;
					ModBase.config.enableRightPants = value;
					SkinLayersModBase.instance.writeConfig();
				},
				() -> ConfigScreenProvider.createConfigScreen(this));

		list.addButtonRow(Component.translatable("gui.tntsallin1client.menu.hud_editor_button"),
				() -> this.minecraft.setScreen(new HudEditorScreen(this)));

		list.addButtonRow(Component.translatable("gui.tntsallin1client.menu.credits_button"),
				() -> this.minecraft.setScreen(new CreditsScreen(this)));

		this.addRenderableWidget(list);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds((this.width - ROW_WIDTH) / 2, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	/** Scrollable row list - {@link ContainerObjectSelectionList}, same base class as vanilla's Controls screen. */
	private static class FeatureList extends ContainerObjectSelectionList<FeatureList.Row> {
		FeatureList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		void addToggleRow(boolean initial, Component label, Consumer<Boolean> onToggle) {
			addToggleRow(initial, label, onToggle, null);
		}

		void addToggleRow(boolean initial, Component label, Consumer<Boolean> onToggle,
				@Nullable Supplier<Screen> optionsScreenFactory) {
			boolean hasOptions = optionsScreenFactory != null;
			int toggleWidth = hasOptions ? ROW_WIDTH - OPTIONS_BUTTON_WIDTH - TOGGLE_GAP : ROW_WIDTH;
			CycleButton<Boolean> toggle = CycleButton.onOffBuilder(initial)
					.create(0, 0, toggleWidth, ROW_HEIGHT, label, (button, value) -> onToggle.accept(value));
			Button options = hasOptions
					? Button.builder(Component.translatable("gui.tntsallin1client.menu.options_button"),
									button -> Minecraft.getInstance().setScreen(optionsScreenFactory.get()))
							.bounds(0, 0, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT)
							.build()
					: null;
			this.addEntry(new Row(toggle, options));
		}

		void addButtonRow(Component label, Runnable onPress) {
			Button button = Button.builder(label, b -> onPress.run()).bounds(0, 0, ROW_WIDTH, ROW_HEIGHT).build();
			this.addEntry(new Row(button, null));
		}

		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final AbstractWidget primary;
			private final @Nullable AbstractWidget secondary;

			Row(AbstractWidget primary, @Nullable AbstractWidget secondary) {
				this.primary = primary;
				this.secondary = secondary;
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				this.primary.setPosition(this.getContentX(), this.getContentY());
				this.primary.render(guiGraphics, mouseX, mouseY, partialTick);
				if (this.secondary != null) {
					this.secondary.setPosition(this.getContentX() + this.primary.getWidth() + TOGGLE_GAP, this.getContentY());
					this.secondary.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return this.secondary == null ? List.of(this.primary) : List.of(this.primary, this.secondary);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return this.secondary == null ? List.of(this.primary) : List.of(this.primary, this.secondary);
			}
		}
	}
}
