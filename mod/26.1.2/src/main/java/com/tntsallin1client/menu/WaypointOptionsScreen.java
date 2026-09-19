package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Waypoint idea: dedicated options screen for the waypoint system, reached from the mod menu's
 * "Waypoints" toggle row - two key rebinds (same "click, then press a key; Escape unbinds"
 * pattern as {@link ZoomOptionsScreen}/{@link SpawnOverlayOptionsScreen}: one for
 * {@link ModKeyBindings#OPEN_WAYPOINTS}, one for the quick-create follow-up
 * {@link ModKeyBindings#CREATE_WAYPOINT}) plus the display toggles and a button into
 * {@link WaypointListScreen} (see {@link WaypointMenuIntegration} for both keys' gameplay hookup).
 */
public class WaypointOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private @Nullable Button createRebindButton;
	private @Nullable KeyMapping awaitingKeyMapping;

	public WaypointOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.waypoint_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointsEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.enabled"),
						(button, value) -> {
							config.waypointsEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.rebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKeyMapping = ModKeyBindings.OPEN_WAYPOINTS;
					this.updateRebindButtonLabels();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.createRebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKeyMapping = ModKeyBindings.CREATE_WAYPOINT;
					this.updateRebindButtonLabels();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabels();
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointShowBeam)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.show_beam"),
						(button, value) -> {
							config.waypointShowBeam = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointShowMarker)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.show_marker"),
						(button, value) -> {
							config.waypointShowMarker = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointShowDistance)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.show_distance"),
						(button, value) -> {
							config.waypointShowDistance = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointFadeNearby)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.fade_nearby"),
						(button, value) -> {
							config.waypointFadeNearby = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointConfirmDelete)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.confirm_delete"),
						(button, value) -> {
							config.waypointConfirmDelete = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.waypoint_options.manage_button"),
						button -> this.minecraft.setScreen(new WaypointListScreen(this)))
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private void updateRebindButtonLabels() {
		updateRebindButtonLabel(this.rebindButton, ModKeyBindings.OPEN_WAYPOINTS, "gui.tntsallin1client.waypoint_options.key");
		updateRebindButtonLabel(this.createRebindButton, ModKeyBindings.CREATE_WAYPOINT, "gui.tntsallin1client.waypoint_options.create_key");
	}

	private void updateRebindButtonLabel(@Nullable Button button, KeyMapping mapping, String labelKey) {
		if (button == null) {
			return;
		}
		Component keyName = mapping.getTranslatedKeyMessage();
		Component label = Component.translatable(labelKey, keyName);
		if (mapping == this.awaitingKeyMapping) {
			label = Component.literal("> ")
					.append(label.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
		}
		button.setMessage(label);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.awaitingKeyMapping != null) {
			this.awaitingKeyMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKeyMapping != null) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			this.awaitingKeyMapping.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
			finishRebind();
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	private void finishRebind() {
		this.awaitingKeyMapping = null;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.updateRebindButtonLabels();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
