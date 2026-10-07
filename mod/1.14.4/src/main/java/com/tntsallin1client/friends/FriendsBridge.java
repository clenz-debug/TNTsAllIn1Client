package com.tntsallin1client.friends;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The mod's side of the launcher's friends file bridge, as far as this version can use it. The
 * launcher alone talks to the friends backend; it hands us online friends, world invitations and
 * "join this server" requests in {@link #INBOX_FILE}, and we leave it commands as single files in
 * {@link #OUTBOX_DIR}.
 *
 * <p>What the other versions do beyond this - inviting friends into the own world - needs e4mc,
 * which only exists from Minecraft 1.17 on; and an invitation always comes from such a world, so
 * from another Minecraft version. Invitations are therefore shown and can be declined, with a note which
 * version they need, but not accepted (unless one should ever name this version).
 *
 * <p>No inbox means the game wasn't started by our launcher with an online account (e.g. offline
 * mode) - then everything friends-related stays hidden.
 */
public final class FriendsBridge {
	/** The version an invitation has to name for it to be joinable from here. */
	public static final String MINECRAFT_VERSION = "1.14.4";

	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final Gson GSON = new Gson();
	private static final String INBOX_FILE = "tntsallin1client-friends.json";
	private static final String OUTBOX_DIR = "tntsallin1client-outbox";
	private static final int TICKS_BETWEEN_CHECKS = 20;

	public static final class Friend {
		public final String name;
		/** "online", "away" or "dnd". */
		public final String status;

		Friend(String name, String status) {
			this.name = name;
			this.status = status;
		}
	}

	public static final class Invite {
		public final String fromUuid;
		public final String fromName;
		public final String address;
		public final String version;
		private final long expires;

		Invite(String fromUuid, String fromName, String address, String version, long expires) {
			this.fromUuid = fromUuid;
			this.fromName = fromName;
			this.address = address;
			this.version = version;
			this.expires = expires;
		}

		String key() {
			return this.fromUuid + "@" + this.expires;
		}

		public boolean isJoinable() {
			return MINECRAFT_VERSION.equals(this.version);
		}
	}

	private static int tickCounter;
	private static long inboxModified = -1;
	private static boolean active;
	private static boolean dnd;
	private static List<Friend> friends = Collections.emptyList();
	private static List<Invite> invites = Collections.emptyList();
	private static final Set<String> SEEN_INVITES = new HashSet<String>();
	private static final Set<String> HANDLED_JOINS = new HashSet<String>();
	private static final AtomicInteger COMMAND_COUNTER = new AtomicInteger();

	private FriendsBridge() {
	}

	/** Started by our launcher with an online account - friends features are available. */
	public static boolean isActive() {
		return active;
	}

	public static List<Friend> onlineFriends() {
		return friends;
	}

	public static List<Invite> invites() {
		return invites;
	}

	public static void tick(Minecraft client) {
		if (++tickCounter < TICKS_BETWEEN_CHECKS) {
			return;
		}
		tickCounter = 0;

		File inbox = new File(configDir(client), INBOX_FILE);
		// 0 for a file that isn't there.
		long modified = inbox.lastModified();
		if (modified == inboxModified) {
			return;
		}
		inboxModified = modified;
		boolean wasActive = active;
		int invitesBefore = invites.size();
		if (modified == 0) {
			active = false;
		} else {
			try {
				read(client, new JsonParser().parse(new String(Files.readAllBytes(inbox.toPath()), StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (IOException | RuntimeException e) {
				// Caught while the launcher was writing it, or not what we expect - read again with its next change.
				LOGGER.warn("Failed to read friends data from " + inbox + ".", e);
			}
		}

		// The title screen's Friends button is only there while friends are active, and names the
		// number of invitations - rebuilt when either changed while that screen is open.
		if ((active != wasActive || invites.size() != invitesBefore) && client.screen instanceof TitleScreen) {
			client.screen.init(client, client.screen.width, client.screen.height);
		}
	}

	private static void read(Minecraft client, JsonObject json) {
		active = true;
		dnd = json.has("dnd") && json.get("dnd").getAsBoolean();

		List<Friend> nextFriends = new ArrayList<Friend>();
		for (JsonElement element : array(json, "friends")) {
			JsonObject friend = element.getAsJsonObject();
			nextFriends.add(new Friend(friend.get("name").getAsString(), friend.get("status").getAsString()));
		}
		friends = Collections.unmodifiableList(nextFriends);

		List<Invite> nextInvites = new ArrayList<Invite>();
		for (JsonElement element : array(json, "invites")) {
			JsonObject invite = element.getAsJsonObject();
			JsonObject from = invite.getAsJsonObject("from");
			nextInvites.add(new Invite(from.get("uuid").getAsString(), from.get("name").getAsString(),
					invite.get("address").getAsString(), invite.get("version").getAsString(), invite.get("expires").getAsLong()));
		}
		invites = Collections.unmodifiableList(nextInvites);

		for (Invite invite : invites) {
			if (SEEN_INVITES.add(invite.key())) {
				notifyInvite(client, invite);
			}
		}

		if (json.has("join") && json.get("join").isJsonObject()) {
			JsonObject join = json.getAsJsonObject("join");
			if (HANDLED_JOINS.add(join.get("id").getAsString())) {
				connect(client, join.get("address").getAsString());
			}
		}
	}

	private static JsonArray array(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : new JsonArray();
	}

	/**
	 * A new world invitation: a pop-up notice in the corner, as in the other versions; on the title
	 * screen the Friends button counts it too. "Do not disturb" keeps quiet - the invitation still
	 * waits on the Friends screen and in the launcher.
	 */
	private static void notifyInvite(Minecraft client, Invite invite) {
		if (dnd) {
			return;
		}
		String hint = invite.isJoinable()
				? I18n.get("gui.tntsallin1client.friends.invite_toast.in_game")
				: I18n.get("gui.tntsallin1client.friends.invite.other_version", invite.version);
		client.getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.TUTORIAL_HINT,
				new TextComponent(I18n.get("gui.tntsallin1client.friends.invite_toast.title", invite.fromName)), new TextComponent(hint)));
	}

	public static void acceptInvite(Minecraft client, Invite invite) {
		sendCommand(client, "dismiss", invite.fromUuid);
		connect(client, invite.address);
	}

	public static void declineInvite(Minecraft client, Invite invite) {
		sendCommand(client, "dismiss", invite.fromUuid);
		List<Invite> remaining = new ArrayList<Invite>(invites);
		remaining.remove(invite);
		invites = Collections.unmodifiableList(remaining);
	}

	/** Leaves the current world or server the way the pause menu's own button does and connects to `address`. */
	public static void connect(Minecraft client, String address) {
		if (client.level != null) {
			boolean local = client.isLocalServer();
			client.level.disconnect();
			if (local) {
				client.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
			} else {
				client.clearLevel();
			}
		}
		client.setScreen(new ConnectScreen(new TitleScreen(), client, new ServerData(address, address, false)));
	}

	/**
	 * Leaves the launcher a command about the friend with the id `from`. Written to a temp file
	 * first and then renamed, so the launcher never reads half a file.
	 */
	private static void sendCommand(Minecraft client, String type, String from) {
		String id = System.currentTimeMillis() + "-" + COMMAND_COUNTER.incrementAndGet();
		JsonObject command = new JsonObject();
		command.addProperty("id", id);
		command.addProperty("type", type);
		command.addProperty("from", from);
		File outbox = new File(configDir(client), OUTBOX_DIR);
		try {
			Files.createDirectories(outbox.toPath());
			File temp = new File(outbox, id + ".tmp");
			Files.write(temp.toPath(), GSON.toJson(command).getBytes(StandardCharsets.UTF_8));
			Files.move(temp.toPath(), new File(outbox, id + ".json").toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOGGER.warn("Failed to hand a friends command to the launcher.", e);
		}
	}

	private static File configDir(Minecraft client) {
		return new File(client.gameDirectory, "config");
	}
}
