package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Waypoint idea follow-up: asks for a name right when creating a waypoint, instead of always
 * starting with a placeholder "Waypoint N" that had to be renamed afterwards in
 * {@link WaypointEditScreen}. Leaving the field empty still falls back to that same "Waypoint N"
 * naming (shown as the field's hint text so the default is visible upfront), rather than forcing
 * a name - same low-friction spirit as every other quick-create action in this mod.
 */
public class WaypointCreateScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final WaypointListScreen parent;
	private @Nullable EditBox nameField;

	public WaypointCreateScreen(WaypointListScreen parent) {
		super(Component.translatable("gui.tntsallin1client.waypoint_create.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.nameField = new EditBox(this.font, x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_edit.name"));
		this.nameField.setMaxLength(24);
		this.nameField.setHint(Component.literal(defaultName()));
		this.addRenderableWidget(this.nameField);
		this.setInitialFocus(this.nameField);
		y += ROW_HEIGHT + 10;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.waypoint_create.create_button"),
						button -> this.create())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	private String defaultName() {
		String worldKey = WaypointScope.currentKey(this.minecraft);
		int existing = worldKey != null ? ClientConfig.get().waypointsFor(worldKey).size() : 0;
		return Component.translatable("gui.tntsallin1client.waypoint_list.default_name", existing + 1).getString();
	}

	private void create() {
		LocalPlayer player = this.minecraft.player;
		String worldKey = WaypointScope.currentKey(this.minecraft);
		if (player == null || this.minecraft.level == null || worldKey == null) {
			this.onClose();
			return;
		}

		ClientConfig config = ClientConfig.get();
		Waypoint waypoint = new Waypoint();
		String typed = this.nameField.getValue().trim();
		waypoint.name = typed.isEmpty() ? defaultName() : typed;
		BlockPos pos = player.blockPosition();
		waypoint.x = pos.getX();
		waypoint.y = pos.getY();
		waypoint.z = pos.getZ();
		waypoint.dimension = this.minecraft.level.dimension().identifier().toString();

		config.waypointsFor(worldKey).add(waypoint);
		config.save();
		this.onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		// isConfirmation() covers both Enter and numpad Enter (InputWithModifiers, verified against
		// the decompiled source - there is no separate "isReturn").
		if (keyEvent.isConfirmation()) {
			this.create();
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		// setScreen re-runs init() on this same WaypointListScreen instance regardless (see its own
		// class doc / ClientMenuScreen's), so a newly created waypoint shows up without extra work.
		this.minecraft.setScreen(this.parent);
	}
}
