package com.tntsallin1client.discord;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * A minimal client for Discord's local connection: it talks to the Discord app running on the same
 * computer and sends the one command the activity display needs. The protocol is small - an 8-byte
 * header (kind of message, length; little-endian) followed by JSON text - and the same as in the
 * Fabric versions' client.
 *
 * <p>Windows only here: Discord listens on a named pipe there, which opens like a file. On macOS
 * and Linux it is a Unix socket, which the Java 8 this version runs on cannot open.
 *
 * <p>Not thread-safe - {@link DiscordPresenceManager} uses it from its one connection thread only.
 */
final class DiscordIpcClient {
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;
	private static final int OP_CLOSE = 2;
	private static final int PIPE_COUNT = 10;

	private static final Gson GSON = new Gson();

	private final ByteChannel channel;

	private DiscordIpcClient(ByteChannel channel) {
		this.channel = channel;
	}

	/** Tries Discord's pipes in turn and says hello on the first that opens. Fails with an exception if Discord isn't running. */
	static DiscordIpcClient connect(long applicationId) throws IOException {
		DiscordIpcClient client = new DiscordIpcClient(openChannel());
		try {
			client.handshake(applicationId);
		} catch (IOException e) {
			client.close();
			throw e;
		}
		return client;
	}

	private static ByteChannel openChannel() throws IOException {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
			throw new IOException("Discord's connection can only be opened on Windows in this version.");
		}
		IOException lastError = null;
		for (int index = 0; index < PIPE_COUNT; index++) {
			try {
				return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw").getChannel();
			} catch (IOException e) {
				lastError = e;
			}
		}
		throw lastError;
	}

	private void handshake(long applicationId) throws IOException {
		JsonObject payload = new JsonObject();
		payload.addProperty("v", 1);
		payload.addProperty("client_id", Long.toString(applicationId));
		writeFrame(OP_HANDSHAKE, payload);
		// Discord answers with a "ready" message - nothing in it matters here, it is only read so it
		// doesn't sit in front of whatever comes later.
		readFrame();
	}

	/** `presence` null takes the activity away. Discord's answers to these are never read - only whether sending worked matters. */
	void setActivity(RichPresenceData presence) throws IOException {
		JsonObject args = new JsonObject();
		args.addProperty("pid", processId());
		args.add("activity", presence != null ? presence.toJson() : JsonNull.INSTANCE);

		JsonObject command = new JsonObject();
		command.addProperty("cmd", "SET_ACTIVITY");
		command.add("args", args);
		command.addProperty("nonce", UUID.randomUUID().toString());
		writeFrame(OP_FRAME, command);
	}

	/** This Java has no direct way to ask for the process id; the runtime's name is "id@computer". */
	private static long processId() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		try {
			return Long.parseLong(name.substring(0, name.indexOf('@')));
		} catch (RuntimeException e) {
			return 0L;
		}
	}

	void close() {
		try {
			writeFrame(OP_CLOSE, new JsonObject());
		} catch (IOException ignored) {
			// Going away either way.
		}
		try {
			this.channel.close();
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
			this.channel.write(buffer);
		}
	}

	private void readFrame() throws IOException {
		ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		readFully(header);
		header.flip();
		header.getInt();
		readFully(ByteBuffer.allocate(header.getInt()));
	}

	private void readFully(ByteBuffer buffer) throws IOException {
		while (buffer.hasRemaining()) {
			if (this.channel.read(buffer) < 0) {
				throw new EOFException("Discord closed the connection.");
			}
		}
	}
}
