package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Waypoint idea: list of all waypoints, reachable either via {@link WaypointOptionsScreen}'s
 * "Manage" button or directly from gameplay via its own keybind (see {@link WaypointMenuIntegration}).
 * Deliberately no search/sections here unlike {@link ClientMenuScreen} - waypoint counts are
 * expected to stay small, a plain scrollable list ({@link ContainerObjectSelectionList}, same
 * base class as everywhere else in this mod) is enough.
 *
 * <p>Each row is just two widgets - a button (name/color/distance, opens {@link WaypointEditScreen})
 * and a compact visibility toggle - same {@code primary + secondary} row shape as
 * {@code ClientMenuScreen.FeatureList.Row}. Deleting lives inside the edit screen instead of a
 * third per-row button, to keep rows from getting cramped.
 *
 * <p>The list is scoped to the current singleplayer save / multiplayer server ({@link WaypointScope}) -
 * this screen is reachable from the title screen too (mod menu button, 5u), where no world is
 * loaded and {@link WaypointScope#currentKey} returns {@code null}; that case shows an explanatory
 * message instead of a list, with the add/delete-all buttons disabled.
 */
public class WaypointListScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 24;
	private static final int VISIBLE_TOGGLE_WIDTH = 56;
	private static final int TOGGLE_GAP = 4;
	private static final int LIST_TOP = 30;
	private static final int FOOTER_HEIGHT = 30;

	private final @Nullable Screen parent;
	private @Nullable String worldKey;

	public WaypointListScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.tntsallin1client.waypoint_list.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.worldKey = WaypointScope.currentKey(this.minecraft);
		List<Waypoint> waypoints = this.worldKey != null ? ClientConfig.get().waypointsFor(this.worldKey) : List.of();

		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT - ROW_HEIGHT - 6;
		WaypointList list = new WaypointList(this.minecraft, this.width, listHeight, LIST_TOP, waypoints, this);
		this.addRenderableWidget(list);

		int buttonX = (this.width - ROW_WIDTH) / 2;
		int newButtonY = LIST_TOP + listHeight + 6;
		int halfWidth = (ROW_WIDTH - TOGGLE_GAP) / 2;
		Button newButton = Button.builder(Component.translatable("gui.tntsallin1client.waypoint_list.new_button"),
						button -> {
							addWaypointAtPlayer();
							this.clearWidgets();
							this.init();
						})
				.bounds(buttonX, newButtonY, halfWidth, ROW_HEIGHT)
				.build();
		newButton.active = this.worldKey != null;
		this.addRenderableWidget(newButton);

		Button deleteAllButton = Button.builder(Component.translatable("gui.tntsallin1client.waypoint_list.delete_all_button"),
						button -> this.confirmDeleteAll())
				.bounds(buttonX + halfWidth + TOGGLE_GAP, newButtonY, halfWidth, ROW_HEIGHT)
				.build();
		deleteAllButton.active = this.worldKey != null && !waypoints.isEmpty();
		this.addRenderableWidget(deleteAllButton);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(buttonX, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Always confirms, regardless of {@link ClientConfig#waypointConfirmDelete} - that setting only
	 * covers deleting a single waypoint (see {@link WaypointEditScreen}), this is a bulk, unrecoverable
	 * action and asks every time. */
	private void confirmDeleteAll() {
		if (this.worldKey == null) {
			return;
		}
		String key = this.worldKey;
		int count = ClientConfig.get().waypointsFor(key).size();
		this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				ClientConfig.get().waypointsFor(key).clear();
				ClientConfig.get().save();
			}
			// setScreen on this same instance re-runs init() regardless (see ClientMenuScreen's own
			// doc comment on the same behavior) - rebuilds the list from the now-cleared config either way.
			this.minecraft.setScreen(this);
		}, Component.translatable("gui.tntsallin1client.waypoint_list.delete_all_confirm_title"),
				Component.translatable("gui.tntsallin1client.waypoint_list.delete_all_confirm_message", count)));
	}

	private void addWaypointAtPlayer() {
		LocalPlayer player = this.minecraft.player;
		if (player == null || this.minecraft.level == null || this.worldKey == null) {
			return;
		}

		ClientConfig config = ClientConfig.get();
		List<Waypoint> waypoints = config.waypointsFor(this.worldKey);
		Waypoint waypoint = new Waypoint();
		waypoint.name = Component.translatable("gui.tntsallin1client.waypoint_list.default_name", waypoints.size() + 1).getString();
		BlockPos pos = player.blockPosition();
		waypoint.x = pos.getX();
		waypoint.y = pos.getY();
		waypoint.z = pos.getZ();
		waypoint.dimension = this.minecraft.level.dimension().identifier().toString();

		waypoints.add(waypoint);
		config.save();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		if (this.worldKey == null) {
			guiGraphics.drawCenteredString(this.font, Component.translatable("gui.tntsallin1client.waypoint_list.no_world"),
					this.width / 2, LIST_TOP + 10, 0xFFAAAAAA);
		} else if (ClientConfig.get().waypointsFor(this.worldKey).isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable("gui.tntsallin1client.waypoint_list.empty"),
					this.width / 2, LIST_TOP + 10, 0xFFAAAAAA);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	/** Builds the "name (dimension) — Nm" label, colored by the waypoint's own color, for a row's button. */
	static Component rowLabel(Waypoint waypoint) {
		Minecraft client = Minecraft.getInstance();
		StringBuilder text = new StringBuilder(waypoint.name)
				.append(" (").append(WaypointDimensions.label(waypoint.dimension).getString()).append(')');

		if (client.level != null && client.player != null
				&& client.level.dimension().identifier().toString().equals(waypoint.dimension)) {
			Vec3 base = new Vec3(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5);
			long distance = Math.round(client.player.position().distanceTo(base));
			text.append(" — ").append(distance).append('m');
		}

		MutableComponent label = Component.literal(text.toString());
		return label.withColor(waypoint.color & 0xFFFFFF);
	}

	private static final class WaypointList extends ContainerObjectSelectionList<WaypointList.Row> {
		WaypointList(Minecraft minecraft, int width, int height, int y, List<Waypoint> waypoints, WaypointListScreen owner) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
			for (Waypoint waypoint : waypoints) {
				this.addEntry(new Row(waypoint, owner));
			}
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final Button editButton;
			private final CycleButton<Boolean> visibleToggle;

			Row(Waypoint waypoint, WaypointListScreen owner) {
				this.editButton = Button.builder(rowLabel(waypoint),
								button -> owner.minecraft.setScreen(new WaypointEditScreen(owner, waypoint)))
						.bounds(0, 0, ROW_WIDTH - VISIBLE_TOGGLE_WIDTH - TOGGLE_GAP, ROW_HEIGHT)
						.build();
				// displayOnlyValue() - same fix as QuickSortOptionsScreen (5z) - an empty name Component
				// through the default NAME_AND_VALUE display state renders vanilla's "%s: %s" template
				// with a blank first half, i.e. a stray leading ": " before "On"/"Off".
				this.visibleToggle = CycleButton.onOffBuilder(waypoint.visible)
						.displayOnlyValue()
						.create(0, 0, VISIBLE_TOGGLE_WIDTH, ROW_HEIGHT, Component.empty(), (button, value) -> {
							waypoint.visible = value;
							ClientConfig.get().save();
						});
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				this.editButton.setPosition(this.getContentX(), this.getContentY());
				this.editButton.render(guiGraphics, mouseX, mouseY, partialTick);
				this.visibleToggle.setPosition(this.getContentX() + this.editButton.getWidth() + TOGGLE_GAP, this.getContentY());
				this.visibleToggle.render(guiGraphics, mouseX, mouseY, partialTick);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.editButton, this.visibleToggle);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.editButton, this.visibleToggle);
			}
		}
	}
}
