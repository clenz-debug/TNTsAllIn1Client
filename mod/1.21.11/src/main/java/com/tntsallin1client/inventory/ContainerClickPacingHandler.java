package com.tntsallin1client.inventory;

import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Defensive safety net: paces outgoing ServerboundContainerClickPacket sends
 * (redirected here by {@link com.tntsallin1client.mixin.MultiPlayerGameModeMixin})
 * so a burst - rapid manual clicking, a drag across several slots, double-click
 * "collect all", or this mod's own {@link InventorySorter} - gets spread across
 * a few client ticks instead of all firing on the same tick, which is what trips
 * some Spigot/Paper servers' packet-rate/anti-dupe limiter ("Too many suspicious
 * packets" kicks). {@code menu.clicked(...)} (client-side prediction) has already
 * run by the time the redirect fires, so this only ever delays the network send,
 * never what the player visually sees.
 *
 * <p>Single-threaded by design, like every other handler in this mod - {@link #enqueue}
 * runs on the client thread (input handling, inside {@code handleContainerInput}),
 * {@link #tick} and the cleanup hooks all run on the client thread too (end-of-tick,
 * screen close, disconnect) - a plain {@link ArrayDeque} needs no synchronization.
 *
 * <p>Known limitation: the creative-mode inventory screen doesn't route item
 * placement through {@code handleContainerInput} at all (it sends a different
 * packet, {@code ServerboundSetCreativeModeSlotPacket}, via a different path) -
 * out of scope here, not what triggered the kicks this was built for.
 */
public final class ContainerClickPacingHandler {
	private static final Deque<Packet<?>> queue = new ArrayDeque<>();

	private ContainerClickPacingHandler() {
	}

	/** Called from the mixin instead of the original {@code connection.send(packet)} call. */
	public static void enqueue(ClientPacketListener connection, Packet<?> packet) {
		if (!ClientConfig.get().containerClickPacingEnabled) {
			connection.send(packet);
			return;
		}
		queue.addLast(packet);
	}

	/** {@code ClientTickEvents.END_CLIENT_TICK} - drains up to the configured per-tick budget, oldest first. */
	public static void tick(Minecraft client) {
		if (queue.isEmpty()) {
			return;
		}
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			// Defensive fallback - dropAll() via the disconnect hook should already have
			// cleared the queue before this could ever be reached.
			queue.clear();
			return;
		}

		if (!ClientConfig.get().containerClickPacingEnabled) {
			// Toggled off mid-session with packets still queued - flush immediately rather
			// than keep pacing them out under a feature the player just turned off.
			sendAll(connection);
			return;
		}

		int budget = Mth.clamp(ClientConfig.get().containerClickPacketsPerTick, 1, 20);
		for (int sent = 0; sent < budget && !queue.isEmpty(); sent++) {
			connection.send(queue.pollFirst());
		}
	}

	/**
	 * Disconnect hook - the connection these packets were captured for is gone; sending them
	 * into a stale/absent connection would be wrong, and a fresh join fully resyncs the
	 * inventory from the server anyway, so dropping is the safe and correct move.
	 */
	public static void dropAll() {
		queue.clear();
	}

	/**
	 * Per-container-screen close - sends everything still queued right away instead of
	 * spreading it over more ticks, keeping the window where local prediction and the
	 * server's copy of that container can drift as small as possible once the player isn't
	 * even looking at it anymore.
	 */
	private static void flushNow(Minecraft client) {
		if (queue.isEmpty()) {
			return;
		}
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			queue.clear();
			return;
		}
		sendAll(connection);
	}

	private static void sendAll(ClientPacketListener connection) {
		while (!queue.isEmpty()) {
			connection.send(queue.pollFirst());
		}
	}

	/** Registers the disconnect + per-container-screen-close cleanup hooks; call once from {@code onInitializeClient()}. */
	public static void registerCleanupHooks() {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> dropAll());

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AbstractContainerScreen<?>) {
				ScreenEvents.remove(screen).register(scr -> flushNow(client));
			}
		});
	}
}
