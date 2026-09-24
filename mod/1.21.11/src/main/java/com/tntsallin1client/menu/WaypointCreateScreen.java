package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
 * a name - same low-friction spirit as every other quick-create action in this mod. Reachable
 * either from {@link WaypointListScreen}'s "New Waypoint" button ({@code parent} set, so closing
 * goes back to the list) or directly from gameplay via
 * {@link com.tntsallin1client.keybind.ModKeyBindings#CREATE_WAYPOINT} ({@code parent} {@code null},
 * so closing just returns to the game - see {@link WaypointMenuIntegration}). Also lets the color
 * be picked upfront via the same {@link ColorPickerPanel} {@link WaypointEditScreen} uses, per user
 * request - previously only settable afterwards by opening the newly created waypoint's own edit
 * screen.
 */
public class WaypointCreateScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final @Nullable WaypointListScreen parent;
	private @Nullable EditBox nameField;
	private @Nullable ColorPickerPanel colorPicker;
	// Mirrors Waypoint's own default (opaque white) until the picker is touched.
	private int pendingColor = 0xFFFFFFFF;

	public WaypointCreateScreen(@Nullable WaypointListScreen parent) {
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

		this.colorPicker = new ColorPickerPanel(this.font, x, y, ROW_WIDTH, this.pendingColor,
				this::addRenderableWidget,
				argb -> this.pendingColor = argb);
		y += ColorPickerPanel.totalHeight() + 10;

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
		return worldKey != null ? defaultName(worldKey)
				: Component.translatable("gui.tntsallin1client.waypoint_list.default_name", 1).getString();
	}

	private static String defaultName(String worldKey) {
		int existing = ClientConfig.get().waypointsFor(worldKey).size();
		return Component.translatable("gui.tntsallin1client.waypoint_list.default_name", existing + 1).getString();
	}

	private void create() {
		createAtPlayer(this.minecraft, this.nameField.getValue(), this.pendingColor);
		this.onClose();
	}

	/** Builds and saves the waypoint at the player's current position. Returns its final name, or {@code null} if there's no player/level/world to create one against. */
	private static @Nullable String createAtPlayer(Minecraft minecraft, @Nullable String typedName, int color) {
		LocalPlayer player = minecraft.player;
		String worldKey = WaypointScope.currentKey(minecraft);
		if (player == null || minecraft.level == null || worldKey == null) {
			return null;
		}

		ClientConfig config = ClientConfig.get();
		Waypoint waypoint = new Waypoint();
		String typed = typedName == null ? "" : typedName.trim();
		waypoint.name = typed.isEmpty() ? defaultName(worldKey) : typed;
		waypoint.color = color;
		BlockPos pos = player.blockPosition();
		waypoint.x = pos.getX();
		waypoint.y = pos.getY();
		waypoint.z = pos.getZ();
		waypoint.dimension = minecraft.level.dimension().identifier().toString();

		config.waypointsFor(worldKey).add(waypoint);
		config.save();
		return waypoint.name;
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
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		if (this.colorPicker != null) {
			this.colorPicker.render(guiGraphics, 0xFFFFFFFF);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.colorPicker != null && this.colorPicker.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.colorPicker != null && this.colorPicker.mouseDragged(event)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.colorPicker != null && this.colorPicker.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		// null parent (opened directly via keybind, not from the list) just returns to the game.
		// A non-null parent's setScreen re-runs init() on that same WaypointListScreen instance
		// regardless (see its own class doc / ClientMenuScreen's), so a newly created waypoint
		// shows up there without extra work.
		this.minecraft.setScreen(this.parent);
	}
}
