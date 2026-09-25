package com.tntsallin1client.friends;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.HttpUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inviting friends into the own singleplayer world (Phase 8b, own user request: nobody gets in
 * without an invitation). The first invitation opens the world through e4mc
 * ({@link E4mcControl}), which gives it a public address; that address only ever goes to the
 * invited friends, through the launcher and our backend. And the actual lock: while an invite
 * session runs, {@code InviteOnlyLoginMixin} turns away everyone at login who wasn't invited - the
 * UUID can't be faked, an opened world runs in online mode.
 *
 * <p>The session ends with the world (server stopping): all invitations are withdrawn.
 */
public final class WorldInvites {
	private static final Logger LOGGER = LogUtils.getLogger();
	/** e4mc usually has its address within a few seconds; give up after this. */
	private static final long ADDRESS_TIMEOUT_MS = 30_000;

	public enum State { NONE, OPENING, OPEN, FAILED }

	private static State state = State.NONE;
	private static @Nullable String address;
	private static @Nullable Component failure;
	private static long openingSince;
	/** We opened this world through e4mc - only then is it invite-only (a normal LAN world stays as it was). */
	private static boolean hostedByUs;
	/** Invited this session - may join (read on the server thread, hence concurrent). */
	private static final Set<UUID> allowed = ConcurrentHashMap.newKeySet();
	/** Invited, but the address isn't there yet - sent once it is. */
	private static final List<String> waiting = new ArrayList<>();
	/** Friend UUID (no dashes) -> the launcher command id of their invitation. */
	private static final Map<String, String> sentCommands = new LinkedHashMap<>();

	private WorldInvites() {
	}

	public static State state() {
		return state;
	}

	public static @Nullable Component failure() {
		return failure;
	}

	/** We opened the world for invitations - only invited players may log in, for as long as it's open. */
	public static boolean isInviteOnly() {
		return hostedByUs;
	}

	public static boolean mayJoin(UUID player) {
		Minecraft mc = Minecraft.getInstance();
		return player.equals(mc.getUser().getProfileId()) || allowed.contains(player);
	}

	/** The launcher's answer for the invitation to this friend: null (not invited / pending), "ok" or an error code. */
	public static @Nullable String inviteResult(String friendUuid) {
		String commandId = sentCommands.get(friendUuid);
		if (commandId == null) return waiting.contains(friendUuid) ? null : "none";
		return FriendsBridge.result(commandId);
	}

	public static boolean isInvited(String friendUuid) {
		return waiting.contains(friendUuid) || sentCommands.containsKey(friendUuid);
	}

	/** Invite button: opens the world through e4mc on first use, sends the invitation once the address is known. */
	public static void invite(Minecraft mc, String friendUuid) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) return;
		allowed.add(toUuid(friendUuid));
		if (state == State.OPEN && address != null) {
			send(friendUuid);
			return;
		}
		if (!waiting.contains(friendUuid)) waiting.add(friendUuid);
		if (state == State.OPENING) return;
		if (hostedByUs) {
			// Opened by us, but e4mc never delivered an address or stopped - can't recover in this session.
			fail(failure != null ? failure : Component.translatable("gui.tntsallin1client.friends.invite.hosting_stopped"));
			return;
		}

		failure = null;
		if (server.isPublished()) {
			// Opened to LAN the normal way before - e4mc only hooks in when the world opens.
			fail(Component.translatable("gui.tntsallin1client.friends.invite.already_lan"));
			return;
		}
		if (!E4mcControl.setHosting(true)) {
			fail(Component.translatable("gui.tntsallin1client.friends.invite.no_e4mc"));
			return;
		}
		boolean published;
		try {
			published = server.publishServer(mc.gameMode.getPlayerMode(), false, HttpUtil.getAvailablePort());
		} finally {
			E4mcControl.setHosting(false);
		}
		if (!published) {
			fail(Component.translatable("gui.tntsallin1client.friends.invite.open_failed"));
			return;
		}
		hostedByUs = true;
		state = State.OPENING;
		openingSince = System.currentTimeMillis();
	}

	/** Withdraws the invitation to one friend and removes them from the world if they're in it. */
	public static void revoke(Minecraft mc, String friendUuid) {
		waiting.remove(friendUuid);
		sentCommands.remove(friendUuid);
		UUID uuid = toUuid(friendUuid);
		allowed.remove(uuid);
		FriendsBridge.sendCommand("revoke", command -> command.addProperty("to", friendUuid));
		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null) {
			server.execute(() -> {
				ServerPlayer player = server.getPlayerList().getPlayer(uuid);
				if (player != null) {
					player.connection.disconnect(Component.translatable("gui.tntsallin1client.friends.invite.revoked_kick"));
				}
			});
		}
	}

	/** From {@code E4mcChatMixin}: e4mc announced the address of the world we just opened. */
	public static void onAddressAssigned(String assigned) {
		if (state != State.OPENING) return;
		address = assigned;
		state = State.OPEN;
		LOGGER.info("World opened for invited friends");
		for (String friendUuid : List.copyOf(waiting)) {
			send(friendUuid);
		}
		waiting.clear();
	}

	/** From {@code E4mcChatMixin}: e4mc stopped hosting (world closed or error). */
	public static void onHostingStopped() {
		if (state == State.OPEN || state == State.OPENING) {
			fail(Component.translatable("gui.tntsallin1client.friends.invite.hosting_stopped"));
		}
	}

	/** Checked every tick while opening - e4mc never answering (no internet, relay down). */
	public static void tick() {
		if (state == State.OPENING && System.currentTimeMillis() - openingSince > ADDRESS_TIMEOUT_MS) {
			fail(Component.translatable("gui.tntsallin1client.friends.invite.no_address"));
		}
	}

	/** World closing - every invitation of this session is withdrawn. */
	public static void onServerStopped() {
		if (hostedByUs || !sentCommands.isEmpty()) {
			FriendsBridge.sendCommand("revokeAll", command -> {
			});
		}
		state = State.NONE;
		hostedByUs = false;
		address = null;
		failure = null;
		allowed.clear();
		waiting.clear();
		sentCommands.clear();
	}

	private static void send(String friendUuid) {
		String hostAddress = address;
		if (hostAddress == null) return;
		String version = SharedConstants.getCurrentVersion().name();
		sentCommands.put(friendUuid, FriendsBridge.sendCommand("invite", command -> {
			command.addProperty("to", friendUuid);
			command.addProperty("address", hostAddress);
			command.addProperty("version", version);
		}));
	}

	/** The lock ({@link #hostedByUs}) deliberately stays on - if the world is open, only invited players stay welcome. */
	private static void fail(Component reason) {
		failure = reason;
		state = State.FAILED;
		waiting.clear();
	}

	private static UUID toUuid(String undashed) {
		String hex = undashed.replace("-", "");
		return UUID.fromString(hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
				+ hex.substring(16, 20) + "-" + hex.substring(20));
	}
}
