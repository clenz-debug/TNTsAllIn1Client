package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.waypoint.Waypoint;
import com.tntsallin1client.waypoint.WaypointDimensions;
import com.tntsallin1client.waypoint.WaypointScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * "New Waypoint": a name, a color and how the waypoint shows, then "Create" (or Enter) puts it where
 * the player stands. Reached from the list's "New Waypoint" button or straight from gameplay by its
 * own key - then leaving it leads back into the game.
 *
 * <p>The name may stay empty: the waypoint is then called "Waypoint N", which the empty field shows
 * as its hint. Beam, marker, distance and fading start as set in the waypoint options.
 */
public class WaypointCreateScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.waypoint_create.";
	private static final String EDIT_KEY = "gui.tntsallin1client.waypoint_edit.";
	private static final int MAX_NAME_LENGTH = 24;

	private String typedName = "";
	/** Holds the color and the four display switches until the waypoint is created. */
	private final Waypoint pending = new Waypoint();

	public WaypointCreateScreen(Screen parent) {
		super(parent, KEY + "title");
		final Waypoint pending = this.pending;
		pending.applyDisplayDefaults(ClientConfig.get());

		addPanel(new TextFieldPanel(WaypointCreateScreen::defaultName, MAX_NAME_LENGTH,
				() -> this.typedName, value -> this.typedName = value, value -> true).focused());
		addColor(() -> pending.color, argb -> pending.color = argb);
		// Same switches, labels and two-per-row layout as on the edit screen.
		addTogglePair(EDIT_KEY + "show_beam", () -> pending.showBeam, value -> pending.showBeam = value,
				EDIT_KEY + "show_marker", () -> pending.showMarker, value -> pending.showMarker = value);
		addTogglePair(EDIT_KEY + "show_distance", () -> pending.showDistance, value -> pending.showDistance = value,
				EDIT_KEY + "fade_nearby", () -> pending.fadeNearby, value -> pending.fadeNearby = value);
		// "Create" above "Cancel", as in the other versions.
		setAction(KEY + "create_button", this::create, false);
	}

	@Override
	protected String backLabelKey() {
		return "gui.cancel";
	}

	/** "Waypoint N", N being one more than there are in this world. */
	private static String defaultName() {
		String worldKey = WaypointScope.currentKey(Minecraft.getInstance());
		int existing = worldKey != null ? ClientConfig.get().waypointsFor(worldKey).size() : 0;
		return I18n.get("gui.tntsallin1client.waypoint_list.default_name", existing + 1);
	}

	private void create() {
		LocalPlayer player = this.minecraft.player;
		String worldKey = WaypointScope.currentKey(this.minecraft);
		if (player != null && worldKey != null) {
			Waypoint waypoint = this.pending;
			String typed = this.typedName.trim();
			waypoint.name = typed.isEmpty() ? defaultName() : typed;
			waypoint.x = Mth.floor(player.x);
			waypoint.y = Mth.floor(player.y);
			waypoint.z = Mth.floor(player.z);
			waypoint.dimension = WaypointDimensions.key(this.minecraft.level);

			ClientConfig config = ClientConfig.get();
			config.waypointsFor(worldKey).add(waypoint);
			config.save();
		}
		back();
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			create();
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}
}
