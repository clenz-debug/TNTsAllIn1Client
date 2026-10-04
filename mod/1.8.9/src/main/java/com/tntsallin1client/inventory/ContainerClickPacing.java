package com.tntsallin1client.inventory;

import java.util.ArrayDeque;
import java.util.Deque;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.util.math.MathHelper;

/**
 * Spreads the inventory click packets over a few game ticks instead of sending a burst all at once -
 * fast clicking, dragging across slots, and above all our own {@link InventorySorter} can otherwise
 * trip a server's limit on packets per tick. The click has already been applied on the client's side
 * by the time its packet gets here (see `ClientPlayerInteractionManagerMixin`), so only the sending
 * is delayed, never what the player sees.
 *
 * <p>Everything here runs on the client thread.
 */
public final class ContainerClickPacing {
	private static final int MIN_PER_TICK = 1;
	private static final int MAX_PER_TICK = 20;

	private static final Deque<Packet<?>> QUEUE = new ArrayDeque<Packet<?>>();
	/** The connection the waiting packets are for. */
	private static ClientPlayNetworkHandler connection;

	private ContainerClickPacing() {
	}

	/** Takes a click packet in place of the game sending it right away. */
	public static void enqueue(ClientPlayNetworkHandler handler, Packet<?> packet) {
		if (connection != handler) {
			// Left over from a server that is gone - a new join gets the inventory sent anew anyway.
			QUEUE.clear();
			connection = handler;
		}
		QUEUE.addLast(packet);
	}

	/** Once per game tick: sends as many of the waiting packets as the setting allows, oldest first. */
	public static void tick(MinecraftClient client) {
		if (QUEUE.isEmpty()) {
			return;
		}
		if (client.getNetworkHandler() != connection) {
			QUEUE.clear();
			return;
		}
		int budget = MathHelper.clamp(ClientConfig.get().containerClickPacketsPerTick, MIN_PER_TICK, MAX_PER_TICK);
		for (int sent = 0; sent < budget && !QUEUE.isEmpty(); sent++) {
			connection.sendPacket(QUEUE.pollFirst());
		}
	}

	/**
	 * Sends everything still waiting at once - called right before the game tells the server that the
	 * inventory screen was closed, which has to come after the clicks made in it.
	 */
	public static void flush() {
		if (MinecraftClient.getInstance().getNetworkHandler() != connection) {
			QUEUE.clear();
			return;
		}
		while (!QUEUE.isEmpty()) {
			connection.sendPacket(QUEUE.pollFirst());
		}
	}
}
