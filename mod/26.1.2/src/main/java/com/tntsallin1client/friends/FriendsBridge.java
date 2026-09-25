package com.tntsallin1client.friends;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.menu.FriendsInGameScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The mod's side of the launcher's friends file bridge (Phase 8/8b, see the launcher's
 * {@code gameActivity.ts}). The launcher alone talks to the friends backend; it hands us online
 * friends, world invitations and "join this server" requests in {@link #INBOX}, and we leave it
 * commands (invite, withdraw, decline) as single files in {@link #OUTBOX}.
 *
 * <p>No inbox means the game wasn't started by our launcher with an online account (e.g. offline
 * mode) - then everything friends-related stays hidden in-game.
 */
public final class FriendsBridge {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new Gson();
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir();
	private static final Path INBOX = CONFIG.resolve("tntsallin1client-friends.json");
	private static final Path OUTBOX = CONFIG.resolve("tntsallin1client-outbox");
	private static final int TICKS_BETWEEN_CHECKS = 20;
	private static final SystemToast.SystemToastId INVITE_TOAST = new SystemToast.SystemToastId(8000L);

	public record Friend(String uuid, String name, String status) {
	}

	public record Invite(String fromUuid, String fromName, String address, String version, long expires) {
		String key() {
			return fromUuid + "@" + expires;
		}
	}

	private static int tickCounter;
	private static long inboxModified = -1;
	private static boolean active;
	private static boolean dnd;
	private static List<Friend> friends = List.of();
	private static List<Invite> invites = List.of();
	private static Map<String, String> results = Map.of();
	private static final Set<String> seenInvites = new HashSet<>();
	private static final Set<String> handledJoins = new HashSet<>();
	private static final AtomicInteger commandCounter = new AtomicInteger();

	private FriendsBridge() {
	}

	/** Everything friends-related in-game (Phase 8/8b) - called once from the mod's init. */
	public static void register() {
		E4mcControl.init();
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			tick(mc);
			WorldInvites.tick();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> WorldInvites.onServerStopped());
		// A small "Friends" button in the pause menu's top left corner - the button rows themselves
		// already carry two layouts (Minecraft/client design), a corner stays clear of both.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!active || !(screen instanceof PauseScreen pauseScreen) || !pauseScreen.showsPauseMenu()) return;
			Component label = invites.isEmpty()
					? Component.translatable("gui.tntsallin1client.friends.title")
					: Component.translatable("gui.tntsallin1client.friends.button_with_invites", invites.size());
			Screens.getWidgets(screen).add(Button.builder(label, button -> client.setScreen(new FriendsInGameScreen(screen)))
					.bounds(6, 6, 90, 20)
					.build());
		});
	}

	/** Launched by our launcher with an online account - friends features are available. */
	public static boolean isActive() {
		return active;
	}

	public static List<Friend> onlineFriends() {
		return friends;
	}

	public static List<Invite> invites() {
		return invites;
	}

	/** The launcher's answer to one of our commands: {@code "ok"}, an error code, or null (pending). */
	public static @Nullable String result(String commandId) {
		return results.get(commandId);
	}

	public static void tick(Minecraft mc) {
		if (++tickCounter < TICKS_BETWEEN_CHECKS) return;
		tickCounter = 0;

		long modified;
		try {
			modified = Files.exists(INBOX) ? Files.getLastModifiedTime(INBOX).toMillis() : -1;
		} catch (IOException e) {
			return;
		}
		if (modified == inboxModified) return;
		inboxModified = modified;
		if (modified < 0) {
			active = false;
			return;
		}
		try {
			read(mc, GSON.fromJson(Files.readString(INBOX), JsonObject.class));
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException | NullPointerException e) {
			LOGGER.warn("Failed to read friends data from {}", INBOX, e);
		}
	}

	private static void read(Minecraft mc, JsonObject json) {
		active = true;
		dnd = json.has("dnd") && json.get("dnd").getAsBoolean();

		List<Friend> nextFriends = new ArrayList<>();
		for (JsonElement element : array(json, "friends")) {
			JsonObject friend = element.getAsJsonObject();
			nextFriends.add(new Friend(friend.get("uuid").getAsString(), friend.get("name").getAsString(), friend.get("status").getAsString()));
		}
		friends = List.copyOf(nextFriends);

		List<Invite> nextInvites = new ArrayList<>();
		for (JsonElement element : array(json, "invites")) {
			JsonObject invite = element.getAsJsonObject();
			JsonObject from = invite.getAsJsonObject("from");
			nextInvites.add(new Invite(from.get("uuid").getAsString(), from.get("name").getAsString(),
					invite.get("address").getAsString(), invite.get("version").getAsString(), invite.get("expires").getAsLong()));
		}
		invites = List.copyOf(nextInvites);

		Map<String, String> nextResults = new HashMap<>();
		if (json.has("results") && json.get("results").isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("results").entrySet()) {
				nextResults.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		results = Map.copyOf(nextResults);

		for (Invite invite : invites) {
			if (seenInvites.add(invite.key())) {
				notifyInvite(mc, invite);
			}
		}

		if (json.has("join") && json.get("join").isJsonObject()) {
			JsonObject join = json.getAsJsonObject("join");
			String id = join.get("id").getAsString();
			if (handledJoins.add(id)) {
				connect(mc, join.get("address").getAsString());
			}
		}
	}

	private static JsonArray array(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : new JsonArray();
	}

	/** A new world invitation: a toast wherever the player is, and on the title screen straight away
	 * the question whether to join. "Do not disturb" keeps quiet - it still waits in the pause menu's
	 * Friends screen and in the launcher. */
	private static void notifyInvite(Minecraft mc, Invite invite) {
		if (dnd) return;
		SystemToast.add(mc.getToastManager(), INVITE_TOAST,
				Component.translatable("gui.tntsallin1client.friends.invite_toast.title", invite.fromName()),
				Component.translatable(mc.level == null
						? "gui.tntsallin1client.friends.invite_toast.menu"
						: "gui.tntsallin1client.friends.invite_toast.in_game"));
		if (mc.level == null && mc.screen instanceof TitleScreen titleScreen) {
			mc.setScreen(new ConfirmScreen(accepted -> {
				if (accepted) {
					acceptInvite(mc, invite);
				} else {
					mc.setScreen(titleScreen);
				}
			}, Component.translatable("gui.tntsallin1client.friends.invite_popup.title", invite.fromName()),
					Component.translatable("gui.tntsallin1client.friends.invite_popup.message", invite.version()),
					Component.translatable("gui.tntsallin1client.friends.join"),
					Component.translatable("gui.tntsallin1client.friends.later")));
		}
	}

	public static void acceptInvite(Minecraft mc, Invite invite) {
		sendCommand("dismiss", command -> command.addProperty("from", invite.fromUuid()));
		connect(mc, invite.address());
	}

	public static void declineInvite(Invite invite) {
		sendCommand("dismiss", command -> command.addProperty("from", invite.fromUuid()));
		invites = invites.stream().filter(other -> !other.equals(invite)).toList();
	}

	/** Leaves the current world or server (saving a singleplayer world like the pause menu's own
	 * button does) and connects to {@code address}. */
	public static void connect(Minecraft mc, String address) {
		mc.execute(() -> {
			if (mc.level != null) {
				mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
			}
			ServerData server = new ServerData(address, address, ServerData.Type.OTHER);
			ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), server, false, null);
		});
	}

	/**
	 * Leaves a command for the launcher and returns its id (to look up {@link #result}). Written to a
	 * temp file first and then renamed, so the launcher never reads half a file.
	 */
	public static String sendCommand(String type, java.util.function.Consumer<JsonObject> fill) {
		String id = System.currentTimeMillis() + "-" + commandCounter.incrementAndGet();
		JsonObject command = new JsonObject();
		command.addProperty("id", id);
		command.addProperty("type", type);
		fill.accept(command);
		try {
			Files.createDirectories(OUTBOX);
			Path temp = OUTBOX.resolve(id + ".tmp");
			Files.writeString(temp, GSON.toJson(command));
			Files.move(temp, OUTBOX.resolve(id + ".json"), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOGGER.warn("Failed to hand a friends command to the launcher", e);
		}
		return id;
	}
}
