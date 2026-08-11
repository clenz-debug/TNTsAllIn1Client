package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
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
 * "Waypoints" toggle row - key rebind (same "click, then press a key; Escape unbinds" pattern as
 * {@link ZoomOptionsScreen}/{@link SpawnOverlayOptionsScreen}) plus the two display toggles and a
 * button into {@link WaypointListScreen}, so the list is reachable both from here and directly
 * from gameplay via {@link ModKeyBindings#OPEN_WAYPOINTS} (see {@link WaypointMenuIntegration}).
 */
public class WaypointOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private boolean awaitingKey;

	public WaypointOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.waypoint_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.rebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKey = true;
					this.updateRebindButtonLabel();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabel();
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

	private void updateRebindButtonLabel() {
		Component keyName = ModKeyBindings.OPEN_WAYPOINTS.getTranslatedKeyMessage();
		Component label = Component.translatable("gui.tntsallin1client.waypoint_options.key", keyName);
		if (this.awaitingKey) {
			label = Component.literal("> ")
					.append(label.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
		}
		this.rebindButton.setMessage(label);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.awaitingKey) {
			ModKeyBindings.OPEN_WAYPOINTS.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKey) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			ModKeyBindings.OPEN_WAYPOINTS.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
			finishRebind();
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	private void finishRebind() {
		this.awaitingKey = false;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.updateRebindButtonLabel();
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
}
