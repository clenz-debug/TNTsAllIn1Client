package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Own user follow-up request ("kann man das über eine Taste aufrufen wie beim Waypoint-Menü") -
 * opens {@link PinnedRecipeListScreen} from gameplay via keybind, same "only while no other screen
 * is open" gate {@link PauseMenuIntegration} uses for {@link ModKeyBindings#OPEN_MENU}, same shape
 * as {@link WaypointMenuIntegration#register}. Gated on {@link ClientConfig#pinnedRecipeEnabled}
 * like every other feature's own keybind here - turning the feature off in the mod menu turns its
 * key off too.
 */
public final class PinnedRecipeMenuIntegration {
	private PinnedRecipeMenuIntegration() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ModKeyBindings.OPEN_PINNED_RECIPES.consumeClick()) {
				if (client.screen == null && ClientConfig.get().pinnedRecipeEnabled) {
					client.setScreen(new PinnedRecipeListScreen(null));
				}
			}
		});
	}
}
