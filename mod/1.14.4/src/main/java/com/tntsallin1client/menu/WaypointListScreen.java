package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * The list of waypoints of the world or server the player is in, reached from the waypoint options
 * ("Manage Waypoints...") or straight from gameplay by its own key. Grouped by dimension - Overworld,
 * Nether, End, then any other a waypoint is in.
 *
 * <p>A waypoint's row is two buttons: its name in its color, with the distance if it is in the
 * player's dimension (opens {@link WaypointEditScreen}), and a small switch for showing it or not.
 * Deleting one waypoint is on its edit screen, so the rows stay uncluttered.
 *
 * <p>The screen is reachable from the title screen too, through the mod menu. No world is loaded
 * there: it says so instead of listing anything, and "New Waypoint" and "Delete All" are disabled.
 */
public class WaypointListScreen extends ClientScreen {
	private static final String KEY = "gui.tntsallin1client.waypoint_list.";
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int HEADER_HEIGHT = 18;
	private static final int VISIBLE_TOGGLE_WIDTH = 56;
	private static final int BUTTON_GAP = 4;
	private static final int LIST_TOP = 30;
	private static final int MESSAGE_COLOR = 0xAAAAAA;
	private static final int LEFT_MOUSE_BUTTON = 0;
	/** The known dimensions come first, in this order; any other follows as its first waypoint appears. */
	private static final String[] DIMENSION_ORDER = {WaypointDimensions.OVERWORLD, WaypointDimensions.NETHER, WaypointDimensions.END};

	private final List<Row> rows = new ArrayList<Row>();
	/** Lives as long as the screen, so the list doesn't jump to the top after a visit to a waypoint's screen. */
	private final ScrollPane pane = new ScrollPane(this::placeRows);
	/** Null outside of a world. */
	private String worldKey;
	private List<Waypoint> waypoints = Collections.emptyList();

	public WaypointListScreen(Screen parent) {
		super(parent, KEY + "title");
	}

	/** Runs again every time the player comes back from another screen - the list is rebuilt from what is there now. */
	@Override
	protected void init() {
		this.worldKey = WaypointScope.currentKey(this.minecraft);
		this.waypoints = this.worldKey != null ? ClientConfig.get().waypointsFor(this.worldKey) : Collections.<Waypoint>emptyList();

		int x = (this.width - ROW_WIDTH) / 2;
		int backY = this.height - 28;
		int actionsY = backY - ROW_SPACING;
		buildRows(x);
		this.pane.layout(LIST_TOP, actionsY - 6, x + ROW_WIDTH + ScrollPane.SCROLLBAR_GAP, contentHeight());

		int halfWidth = (ROW_WIDTH - BUTTON_GAP) / 2;
		Button newButton = this.addButton(new Button(x, actionsY, halfWidth, ROW_HEIGHT, I18n.get(KEY + "new_button"),
				pressed -> this.minecraft.setScreen(new WaypointCreateScreen(this))));
		newButton.active = this.worldKey != null;
		Button deleteAllButton = this.addButton(new Button(x + ROW_WIDTH - halfWidth, actionsY, halfWidth, ROW_HEIGHT,
				I18n.get(KEY + "delete_all_button"), pressed -> askDeleteAll()));
		deleteAllButton.active = !this.waypoints.isEmpty();
		// In the client design "Back" is in the top left corner, like on the options screens.
		this.addButton(TopBar.inUse()
				? TopBar.back(I18n.get("gui.back"), this::back)
				: new Button(x, backY, ROW_WIDTH, ROW_HEIGHT, I18n.get("gui.back"), pressed -> back()));
	}

	/**
	 * Always asks, whatever "Confirm Before Delete" says - that one is about a single waypoint, this
	 * can't be taken back. Either way the player is back on this screen afterwards.
	 */
	private void askDeleteAll() {
		this.minecraft.setScreen(new ThemedConfirmScreen(confirmed -> {
			if (confirmed && this.worldKey != null) {
				ClientConfig config = ClientConfig.get();
				config.waypointsFor(this.worldKey).clear();
				config.save();
			}
			this.minecraft.setScreen(this);
		}, I18n.get(KEY + "delete_all_confirm_title"),
				I18n.get(KEY + "delete_all_confirm_message", this.waypoints.size())));
	}

	private void buildRows(int x) {
		Map<String, List<Waypoint>> byDimension = new LinkedHashMap<String, List<Waypoint>>();
		for (String dimension : DIMENSION_ORDER) {
			byDimension.put(dimension, new ArrayList<Waypoint>());
		}
		for (Waypoint waypoint : this.waypoints) {
			List<Waypoint> section = byDimension.get(waypoint.dimension);
			if (section == null) {
				section = new ArrayList<Waypoint>();
				byDimension.put(waypoint.dimension, section);
			}
			section.add(waypoint);
		}

		this.rows.clear();
		int y = 0;
		for (Map.Entry<String, List<Waypoint>> section : byDimension.entrySet()) {
			if (section.getValue().isEmpty()) {
				continue;
			}
			this.rows.add(new Row(WaypointDimensions.label(section.getKey()), y));
			y += HEADER_HEIGHT;
			for (Waypoint waypoint : section.getValue()) {
				this.rows.add(new Row(waypoint, x, y));
				y += ROW_SPACING;
			}
		}
	}

	private int contentHeight() {
		if (this.rows.isEmpty()) {
			return 0;
		}
		// The last row is always a waypoint - a heading only exists above one.
		return this.rows.get(this.rows.size() - 1).y + ROW_HEIGHT;
	}

	private void placeRows() {
		for (Row row : this.rows) {
			if (row.edit != null) {
				row.edit.y = screenY(row);
				row.toggle.y = screenY(row);
			}
		}
	}

	private int screenY(Row row) {
		return LIST_TOP + row.y - this.pane.offset();
	}

	/** "Name - 12m" - the distance only for a waypoint in the dimension the player is in. */
	private static String rowLabel(Waypoint waypoint) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null || !WaypointDimensions.key(client.level).equals(waypoint.dimension)) {
			return waypoint.name;
		}
		double deltaX = client.player.x - (waypoint.x + 0.5);
		double deltaY = client.player.y - (waypoint.y + 0.5);
		double deltaZ = client.player.z - (waypoint.z + 0.5);
		return waypoint.name + " - " + Math.round(Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)) + "m";
	}

	private static String visibleLabel(Waypoint waypoint) {
		return I18n.get(waypoint.visible ? "options.on" : "options.off");
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		this.pane.mouseScrolled(amount);
		return true;
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		int mouseX = (int) x;
		int mouseY = (int) y;
		if (button == LEFT_MOUSE_BUTTON && this.pane.contains(mouseY)) {
			if (this.pane.mouseClicked(mouseX, mouseY)) {
				return true;
			}
			for (Row row : this.rows) {
				if (row.edit == null) {
					continue;
				}
				if (row.edit.isMouseOver(x, y)) {
					row.edit.playDownSound(this.minecraft.getSoundManager());
					this.minecraft.setScreen(new WaypointEditScreen(this, row.waypoint));
					return true;
				}
				if (row.toggle.isMouseOver(x, y)) {
					row.toggle.playDownSound(this.minecraft.getSoundManager());
					row.waypoint.visible = !row.waypoint.visible;
					ClientConfig.get().save();
					row.toggle.setMessage(visibleLabel(row.waypoint));
					return true;
				}
			}
		}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
		this.pane.mouseDragged((int) y);
		return super.mouseDragged(x, y, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		this.pane.mouseReleased();
		return super.mouseReleased(x, y, button);
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		super.render(mouseX, mouseY, partialTick);
		if (this.worldKey == null) {
			MenuText.centered(I18n.get(KEY + "no_world"), this.width / 2, LIST_TOP + 10, MESSAGE_COLOR);
		} else if (this.waypoints.isEmpty()) {
			MenuText.centered(I18n.get(KEY + "empty"), this.width / 2, LIST_TOP + 10, MESSAGE_COLOR);
		}

		// A button scrolled half out of view must not light up under a cursor that is on the title or on the buttons below.
		int hoverY = this.pane.contains(mouseY) ? mouseY : -1;
		this.pane.beginClip(this.minecraft);
		for (Row row : this.rows) {
			if (row.edit == null) {
				MenuText.centered(row.heading, this.width / 2, screenY(row) + 4, MESSAGE_COLOR);
			} else {
				row.edit.render(mouseX, hoverY, partialTick);
				row.toggle.render(mouseX, hoverY, partialTick);
			}
		}
		this.pane.endClip();
		this.pane.renderScrollbar();
	}

	/** A dimension's heading, or a waypoint with its two buttons. */
	private static final class Row {
		final String heading;
		final Waypoint waypoint;
		/** Top edge, counted from the top of the list. */
		final int y;
		final Button edit;
		final Button toggle;

		Row(String heading, int y) {
			this.heading = heading;
			this.waypoint = null;
			this.y = y;
			this.edit = null;
			this.toggle = null;
		}

		Row(Waypoint waypoint, int x, int y) {
			this.heading = null;
			this.waypoint = waypoint;
			this.y = y;
			int editWidth = ROW_WIDTH - VISIBLE_TOGGLE_WIDTH - BUTTON_GAP;
			// Clicked by the screen itself, not by the game - so the buttons need no action of their own.
			this.edit = new ColoredButton(x, editWidth, ROW_HEIGHT, rowLabel(waypoint), waypoint.color & 0xFFFFFF);
			this.toggle = new Button(x + editWidth + BUTTON_GAP, 0, VISIBLE_TOGGLE_WIDTH, ROW_HEIGHT, visibleLabel(waypoint), pressed -> { });
		}
	}

	/** A button whose text has a color of its own - the game's buttons only know white, and yellow under the cursor. */
	private static final class ColoredButton extends Button {
		private static final int TEXT_MARGIN = 4;

		private final String text;
		private final int color;

		ColoredButton(int x, int width, int height, String text, int color) {
			// The game draws the button without a text of its own.
			super(x, 0, width, height, "", pressed -> { });
			this.text = text;
			this.color = color;
		}

		@Override
		public void renderButton(int mouseX, int mouseY, float partialTick) {
			super.renderButton(mouseX, mouseY, partialTick);
			if (ThemedUi.active()) {
				// The waypoint's own color, in the client design's font.
				ClientFont.drawCentered(ClientFont.fit(this.text, this.width - 2 * TEXT_MARGIN), this.x + this.width / 2.0F,
						this.y + (this.height - ClientFont.HEIGHT) / 2.0F, 0xFF000000 | this.color);
				return;
			}
			Font font = Minecraft.getInstance().font;
			String shown = font.substrByWidth(this.text, this.width - 2 * TEXT_MARGIN);
			this.drawCenteredString(font, shown, this.x + this.width / 2, this.y + (this.height - 8) / 2, this.color);
		}

		/** The narrator reads a button's own text, which this one keeps empty. */
		@Override
		protected String getNarrationMessage() {
			return this.text;
		}
	}
}
