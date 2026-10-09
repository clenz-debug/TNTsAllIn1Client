package com.tntsallin1client.discord;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Minimal Discord "local RPC" (IPC) client - connects to the Discord desktop client running on the
 * same machine over a named pipe (Windows) / Unix domain socket (Mac/Linux) and sends `SET_ACTIVITY`
 * commands to drive Rich Presence. Deliberately hand-rolled instead of pulling in a third-party
 * library (the two most commonly cited ones for this, jagrosh/DiscordIPC and its forks, are
 * effectively unmaintained - the upstream README still points at JCenter, which shut down years ago,
 * and shipping it would mean pinning an arbitrary JitPack-built commit plus correctly jar-in-jar
 * bundling its own transitive deps (org.json, slf4j-api, junixsocket) through Loom, none of which is
 * verifiable without a full build+live-Discord test). The actual protocol is small and stable enough
 * that reimplementing just the one command this mod needs is both safer and lighter than that:
 * an 8-byte little-endian header (opcode, payload length) followed by a UTF-8 JSON payload, same
 * framing Discord's own official SDKs use. JSON itself goes through Gson, already a hard dependency
 * of the game itself (see every other JSON use in this codebase, e.g. {@code ClientConfig}) - no new
 * library needed either way.
 *
 * <p>Not thread-safe by itself - {@link com.tntsallin1client.discord.DiscordPresenceManager} is the
 * only caller, and only ever from its own single IPC thread (the handshake can block indefinitely,
 * see there).
 */
public final class DiscordIpcClient implements AutoCloseable {
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;
	private static final int OP_CLOSE = 2;

	private static final Gson GSON = new Gson();

	private final @Nullable ByteChannel channel;

	private DiscordIpcClient(ByteChannel channel) {
		this.channel = channel;
	}

	/**
	 * Tries every well-known local Discord IPC endpoint in turn (Discord itself, plus PTB/Canary,
	 * and - Windows only needing one form, Unix needing several candidate base directories - picks
	 * whichever connects first) and completes the handshake. Throws (rather than returning null) on
	 * total failure so the caller's single try/catch covers "Discord isn't running" the same way it
	 * covers every other connection problem - own user request: quietly retry in the background
	 * rather than surfacing this as an error anywhere in-game.
	 */
	public static DiscordIpcClient connect(long applicationId) throws IOException {
		ByteChannel channel = openChannel();
		DiscordIpcClient client = new DiscordIpcClient(channel);
		try {
			client.handshake(applicationId);
		} catch (IOException e) {
			client.close();
			throw e;
		}
		return client;
	}

	private static ByteChannel openChannel() throws IOException {
		if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
			IOException lastError = null;
			for (int i = 0; i < 10; i++) {
				try {
					RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
					return pipe.getChannel();
				} catch (IOException e) {
					lastError = e;
				}
			}
			throw lastError != null ? lastError : new IOException("No Discord IPC pipe found.");
		}

		// Mac/Linux: Discord's own SDKs look for the socket under $XDG_RUNTIME_DIR first, falling
		// back through the other temp-dir env vars a sandboxed (Flatpak/Snap) or plain install might
		// use instead - same candidate list Discord's official game-sdk documents.
		List<String> baseDirs = List.of(
				System.getenv("XDG_RUNTIME_DIR"),
				System.getenv("TMPDIR"),
				System.getenv("TMP"),
				System.getenv("TEMP"),
				"/tmp"
		);
		IOException lastError = null;
		for (String base : baseDirs) {
			if (base == null || base.isBlank()) continue;
			for (int i = 0; i < 10; i++) {
				Path socketPath = Path.of(base, "discord-ipc-" + i);
				try {
					SocketChannel socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
					socketChannel.connect(UnixDomainSocketAddress.of(socketPath));
					return socketChannel;
				} catch (IOException e) {
					lastError = e;
				}
			}
		}
		throw lastError != null ? lastError : new IOException("No Discord IPC socket found.");
	}

	private void handshake(long applicationId) throws IOException {
		JsonObject payload = new JsonObject();
		payload.addProperty("v", 1);
		payload.addProperty("client_id", Long.toString(applicationId));
		writeFrame(OP_HANDSHAKE, payload);
		// Discord replies with a READY dispatch frame - not parsed (nothing in it changes what we'd
		// do next), just drained so it doesn't sit as stray unread data in front of the first real
		// SET_ACTIVITY response later.
		readFrame();
	}

	/**
	 * @param presence {@code null} clears the activity entirely (e.g. Discord privacy setting, or
	 *                 this mod's own toggle turned off mid-session) rather than leaving a stale one
	 *                 shown after the game can no longer update it.
	 */
	public void setActivity(@Nullable RichPresenceData presence) throws IOException {
		JsonObject args = new JsonObject();
		args.addProperty("pid", ProcessHandle.current().pid());
		args.add("activity", presence != null ? presence.toJson() : JsonNull.INSTANCE);

		JsonObject command = new JsonObject();
		command.addProperty("cmd", "SET_ACTIVITY");
		command.add("args", args);
		command.addProperty("nonce", UUID.randomUUID().toString());
		writeFrame(OP_FRAME, command);
		// Discord acknowledges every SET_ACTIVITY with a response frame too, same as the handshake's
		// READY above - deliberately not read here. This client never reads again after the initial
		// handshake, so those acks simply accumulate unread in the OS-level pipe/socket buffer for
		// the rest of the session; at a few hundred bytes each and at most a handful of updates per
		// play session (see DiscordPresenceManager's own change-detection before calling this), that
		// never approaches a buffer size Windows/the kernel would balk at. Reading them back out would
		// need a dedicated reader (thread or non-blocking loop) for zero actual benefit - nothing
		// currently cares whether an update was accepted, only whether *sending* it failed (an
		// IOException here already means "connection's dead, reconnect", the same signal either way).
	}

	@Override
	public void close() {
		if (channel == null) return;
		try {
			writeFrame(OP_CLOSE, new JsonObject());
		} catch (IOException ignored) {
			// Already going away either way - nothing left to react to a failed goodbye with.
		}
		try {
			channel.close();
		} catch (IOException ignored) {
		}
	}

	private void writeFrame(int opcode, JsonObject payload) throws IOException {
		byte[] json = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(8 + json.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(opcode);
		buffer.putInt(json.length);
		buffer.put(json);
		buffer.flip();
		while (buffer.hasRemaining()) {
			channel.write(buffer);
		}
	}

	private void readFrame() throws IOException {
		ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		readFully(header);
		header.flip();
		header.getInt(); // opcode - unused, see the two callers' own doc comments for why
		int length = header.getInt();
		readFully(ByteBuffer.allocate(length));
	}

	private void readFully(ByteBuffer buffer) throws IOException {
		while (buffer.hasRemaining()) {
			if (channel.read(buffer) < 0) {
				throw new EOFException("Discord closed the IPC connection.");
			}
		}
	}
}
