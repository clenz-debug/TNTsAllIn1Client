package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.debug.QuickInfoDebugEntry;
import me.pepperbell.continuity.api.client.ContinuityFeatureStates;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5e: ingame mod menu, top level. Just the on/off switch per feature.
 * Features with more to configure than a toggle get their own dedicated
 * options screen (e.g. {@link MaterialCounterOptionsScreen}), opened via a
 * small button next to that feature's toggle - deliberately not one shared
 * options screen for every feature, which would turn into an unrelated,
 * ever-growing list as more features gain settings. Reachable via the pause
 * menu button ({@link PauseMenuIntegration}) or its own keybind
 * ({@link com.tntsallin1client.keybind.ModKeyBindings#OPEN_MENU}).
 */
public class ClientMenuScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int OPTIONS_BUTTON_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int TOGGLE_WIDTH = ROW_WIDTH - OPTIONS_BUTTON_WIDTH - TOGGLE_GAP;

	private final @Nullable Screen parent;

	public ClientMenuScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.tntsallin1client.menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.coordinatesHudEnabled)
				.create(x, y, TOGGLE_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.coordinates_hud"),
						(button, value) -> {
							config.coordinatesHudEnabled = value;
							config.save();
						}));
		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.menu.options_button"),
						button -> this.minecraft.setScreen(new CoordinatesHudOptionsScreen(this)))
				.bounds(x + TOGGLE_WIDTH + TOGGLE_GAP, y, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.materialCounterEnabled)
				.create(x, y, TOGGLE_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.material_counter"),
						(button, value) -> {
							config.materialCounterEnabled = value;
							config.save();
						}));
		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.menu.options_button"),
						button -> this.minecraft.setScreen(new MaterialCounterOptionsScreen(this)))
				.bounds(x + TOGGLE_WIDTH + TOGGLE_GAP, y, OPTIONS_BUTTON_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.quickSortEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.quick_sort"),
						(button, value) -> {
							config.quickSortEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.f3QuickInfoEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.f3_quick_info"),
						(button, value) -> {
							config.f3QuickInfoEnabled = value;
							config.save();
							QuickInfoDebugEntry.applyVanillaEntryVisibility(this.minecraft);
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.clientNameLabelEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.client_name_label"),
						(button, value) -> {
							config.clientNameLabelEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		ContinuityFeatureStates.FeatureState connectedTextures = ContinuityFeatureStates.get().getConnectedTexturesState();
		this.addRenderableWidget(CycleButton.onOffBuilder(connectedTextures.isEnabled())
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.connected_textures"),
						(button, value) -> {
							if (value) {
								connectedTextures.enable();
							} else {
								connectedTextures.disable();
							}
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.menu.hud_editor_button"),
						button -> this.minecraft.setScreen(new HudEditorScreen(this)))
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
