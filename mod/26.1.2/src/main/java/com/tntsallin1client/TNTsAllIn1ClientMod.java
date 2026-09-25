package com.tntsallin1client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import com.tntsallin1client.compat.EssentialCompat;
import com.tntsallin1client.debug.QuickInfoDebugEntry;
import com.tntsallin1client.debug.SystemInfoOverlay;
import com.tntsallin1client.discord.DiscordPresenceManager;
import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.friends.ActivityReporter;
import com.tntsallin1client.fullbright.FullbrightHandler;
import com.tntsallin1client.hud.ArmorStatusBundledHud;
import com.tntsallin1client.hud.ArmorStatusSlot;
import com.tntsallin1client.hud.ArmorStatusSlotHud;
import com.tntsallin1client.hud.CoordinatesHud;
import com.tntsallin1client.hud.FpsCounterHud;
import com.tntsallin1client.hud.FreecamHud;
import com.tntsallin1client.hud.ItemCounterHud;
import com.tntsallin1client.hud.KeystrokesHud;
import com.tntsallin1client.hud.LatencyHud;
import com.tntsallin1client.inventory.ContainerClickPacingHandler;
import com.tntsallin1client.inventory.QuickSortUi;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.menu.PauseMenuIntegration;
import com.tntsallin1client.menu.PinnedRecipeMenuIntegration;
import com.tntsallin1client.menu.TitleScreenIntegration;
import com.tntsallin1client.menu.WaypointMenuIntegration;
import com.tntsallin1client.recipe.PinnedRecipeHud;
import com.tntsallin1client.recipe.PinnedRecipeManager;
import com.tntsallin1client.screenshot.ScreenshotWatcher;
import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import com.tntsallin1client.spawnoverlay.SpawnOverlayRenderer;
import com.tntsallin1client.waypoint.WaypointRenderer;
import com.tntsallin1client.zoom.ZoomHandler;

public class TNTsAllIn1ClientMod implements ClientModInitializer {
	public static final String MOD_ID = "tntsallin1client";

	// This logger is used to write text to the console and the log file.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("[{}] Hello Fabric world!", MOD_ID);

		// Phase 1 "done" check: a log line and a chat message when a world is joined.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			LOGGER.info("[{}] Joined a world.", MOD_ID);
			client.player.sendSystemMessage(Component.nullToEmpty("TNT's All-In-1 Client loaded."));
		});

		// Phase 5a: coordinates + compass HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "coordinates_hud"), new CoordinatesHud());

		// Phase 5b, renamed 5ag: item counter HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "item_counter_hud"), new ItemCounterHud());

		// Phase 5g: FPS counter HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "fps_counter_hud"), new FpsCounterHud());

		// Own wishlist item: latency (ping) counter HUD, same shape as the FPS counter above.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "latency_hud"), new LatencyHud());

		// Phase 5j: fullbright toggle.
		ClientTickEvents.END_CLIENT_TICK.register(FullbrightHandler::tick);

		// Phase 5j redesigned: key-triggered mob-spawn overlay (was the light-level HUD line).
		SpawnOverlayRenderer.register();

		// Phase 5m: keystrokes overlay.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "keystrokes_hud"), new KeystrokesHud());

		// Phase 5n: screenshot Open/Copy toast.
		ClientTickEvents.END_CLIENT_TICK.register(ScreenshotWatcher::tick);

		// Phase 5c: inventory quick-sort (button + keybind).
		ModKeyBindings.register();
		QuickSortUi.register();

		// Defensive: paces outgoing container-click packets so a burst can't trip a server's
		// packet-rate/anti-dupe limiter ("Too many suspicious packets" kicks) - see
		// ContainerClickPacingHandler and MultiPlayerGameModeMixin.
		ClientTickEvents.END_CLIENT_TICK.register(ContainerClickPacingHandler::tick);
		ContainerClickPacingHandler.registerCleanupHooks();

		// Phase 5e: ingame mod menu (keybind + pause menu button).
		PauseMenuIntegration.register();

		// Phase 5u: mod menu button on the title screen too.
		TitleScreenIntegration.register();

		// Phase 5d: extra "Quick Info" block on the F3 debug screen, plus a
		// separate F3+S page for CPU/GPU/version (moved out of the main screen).
		Identifier quickInfoId = Identifier.fromNamespaceAndPath(MOD_ID, "quick_info");
		DebugScreenEntries.register(quickInfoId, new QuickInfoDebugEntry());
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "system_info_overlay"), new SystemInfoOverlay());
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// Essential's keys overlap with ours - unbind them once, see EssentialCompat.
			EssentialCompat.unbindNewKeys(client);
			client.debugEntries.setStatus(quickInfoId, DebugScreenEntryStatus.IN_OVERLAY);
			QuickInfoDebugEntry.applyVanillaEntryVisibility(client);
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.options.keyDebugModifier.isDown() && ModKeyBindings.SYSTEM_INFO.consumeClick()) {
				SystemInfoOverlay.visible = !SystemInfoOverlay.visible;
			}
		});

		// Phase 5h: hold-to-zoom.
		ClientTickEvents.END_CLIENT_TICK.register(ZoomHandler::tick);

		// Freecam: keybind-toggled detached camera, see FreecamHandler.
		ClientTickEvents.END_CLIENT_TICK.register(FreecamHandler::tick);

		// Phase 8: where in the game the player is, for the launcher's friends presence.
		ClientTickEvents.END_CLIENT_TICK.register(ActivityReporter::tick);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "freecam_hud"), new FreecamHud());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			FreecamHandler.exit(client);
			FreecamHandler.resetWarning();
		});
		// First Escape while freecam is active exits freecam instead of opening the pause menu -
		// let vanilla start opening PauseScreen as normal, then intercept it before it finishes
		// initializing and reset to no screen; a second Escape (freecam now off) opens it normally.
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof PauseScreen && FreecamHandler.isActive()) {
				FreecamHandler.exit(client);
				client.setScreen(null);
			}
		});

		// Phase 5k: hold-to-preview shulker box contents.
		ShulkerPreviewRenderer.registerScreenTracking();

		// Phase 5ah: pin a recipe from the recipe book, shown as a movable HUD reminder, reachable
		// via its own list/management screen directly from gameplay too (own user follow-up request).
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "pinned_recipe_hud"), new PinnedRecipeHud());
		PinnedRecipeManager.register();
		PinnedRecipeMenuIntegration.register();

		// Waypoint system: in-world beam/label markers plus their own list/edit menu,
		// reachable from the mod menu or directly via their own keybind.
		WaypointRenderer.register();
		WaypointMenuIntegration.register();

		// Armor & Tool Status: durability/stack-count of worn armor + mainhand/offhand, either as
		// six individually draggable elements or bundled into one (see ArmorStatusLayoutMode) -
		// both are registered unconditionally, each checks the active mode itself before rendering.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "armor_status_bundled_hud"), new ArmorStatusBundledHud());
		for (ArmorStatusSlot slot : ArmorStatusSlot.values()) {
			Identifier slotId = Identifier.fromNamespaceAndPath(MOD_ID, "armor_status_" + slot.name().toLowerCase(Locale.ROOT) + "_hud");
			HudElementRegistry.addLast(slotId, new ArmorStatusSlotHud(slot));
		}

		// Discord Rich Presence - reconnect/state-update loop, entirely inert (one cheap enabled-flag
		// check) whenever the feature's own toggle is off. See DiscordPresenceManager's own doc
		// comment for why this needs to be tick-driven rather than hung off specific events.
		ClientTickEvents.END_CLIENT_TICK.register(DiscordPresenceManager::tick);
	}
}
