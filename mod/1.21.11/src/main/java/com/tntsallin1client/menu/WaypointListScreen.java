package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Waypoint idea: list of all waypoints, reachable either via {@link WaypointOptionsScreen}'s
 * "Manage" button or directly from gameplay via its own keybind (see {@link WaypointMenuIntegration}).
 * A plain scrollable list ({@link ContainerObjectSelectionList}, same base class as everywhere
 * else in this mod), grouped into per-dimension sections (Overworld/Nether/End first, then any
 * other dimension a waypoint happens to be in) - same in-list-header idea as
 * {@code ClientMenuScreen.FeatureList}'s sections, added after user feedback that a flat list
 * across all dimensions got confusing once there were more than a couple of waypoints.
 *
 * <p>Each waypoint row is just two widgets - a button (name/color/distance, opens
 * {@link WaypointEditScreen}) and a compact visibility toggle. Deleting lives inside the edit
 * screen instead of a third per-row button, to keep rows from getting cramped.
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

	/** Fixed preferred section order - any other (e.g. modded) dimension a waypoint is in still
	 * gets its own section, just appended after these three, in first-appearance order. */
	private static final List<String> DIMENSION_ORDER =
			List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

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
						button -> this.minecraft.setScreen(new WaypointCreateScreen(this)))
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

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		if (this.worldKey == null) {
			MenuText.centered(guiGraphics, this.font, Component.translatable("gui.tntsallin1client.waypoint_list.no_world"),
					this.width / 2, LIST_TOP + 10, 0xFFAAAAAA);
		} else if (ClientConfig.get().waypointsFor(this.worldKey).isEmpty()) {
			MenuText.centered(guiGraphics, this.font, Component.translatable("gui.tntsallin1client.waypoint_list.empty"),
					this.width / 2, LIST_TOP + 10, 0xFFAAAAAA);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	/** Builds the "name — Nm" label, colored by the waypoint's own color, for a row's button - no
	 * dimension suffix anymore, that's now expressed by which section the row sits in. */
	static Component rowLabel(Waypoint waypoint) {
		Minecraft client = Minecraft.getInstance();
		StringBuilder text = new StringBuilder(waypoint.name);

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

			Map<String, List<Waypoint>> byDimension = new LinkedHashMap<>();
			for (String dimension : DIMENSION_ORDER) {
				byDimension.put(dimension, new ArrayList<>());
			}
			for (Waypoint waypoint : waypoints) {
				byDimension.computeIfAbsent(waypoint.dimension, key -> new ArrayList<>()).add(waypoint);
			}

			for (Map.Entry<String, List<Waypoint>> section : byDimension.entrySet()) {
				if (section.getValue().isEmpty()) {
					continue;
				}
				this.addEntry(Row.header(WaypointDimensions.label(section.getKey()), minecraft.font));
				for (Waypoint waypoint : section.getValue()) {
					this.addEntry(Row.waypoint(waypoint, owner));
				}
			}
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		/** Same {@code primary + secondary, or header-only} shape as {@code ClientMenuScreen.FeatureList.Row}. */
		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final AbstractWidget primary;
			private final @Nullable AbstractWidget secondary;
			private final boolean header;

			private Row(AbstractWidget primary, @Nullable AbstractWidget secondary, boolean header) {
				this.primary = primary;
				this.secondary = secondary;
				this.header = header;
			}

			static Row header(Component label, Font font) {
				FocusableTextWidget widget = FocusableTextWidget.builder(label, font)
						.alwaysShowBorder(false)
						.backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
						.build();
				return new Row(widget, null, true);
			}

			static Row waypoint(Waypoint waypoint, WaypointListScreen owner) {
				Button editButton = Button.builder(rowLabel(waypoint),
								button -> owner.minecraft.setScreen(new WaypointEditScreen(owner, waypoint)))
						.bounds(0, 0, ROW_WIDTH - VISIBLE_TOGGLE_WIDTH - TOGGLE_GAP, ROW_HEIGHT)
						.build();
				// displayOnlyValue() - same fix as QuickSortOptionsScreen (5z) - an empty name Component
				// through the default NAME_AND_VALUE display state renders vanilla's "%s: %s" template
				// with a blank first half, i.e. a stray leading ": " before "On"/"Off".
				CycleButton<Boolean> visibleToggle = CycleButton.onOffBuilder(waypoint.visible)
						.displayOnlyValue()
						.create(0, 0, VISIBLE_TOGGLE_WIDTH, ROW_HEIGHT, Component.empty(), (button, value) -> {
							waypoint.visible = value;
							ClientConfig.get().save();
						});
				return new Row(editButton, visibleToggle, false);
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				if (this.header) {
					this.primary.setPosition(this.getContentX() + (ROW_WIDTH - this.primary.getWidth()) / 2,
							this.getContentBottom() - this.primary.getHeight());
				} else {
					this.primary.setPosition(this.getContentX(), this.getContentY());
				}
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
