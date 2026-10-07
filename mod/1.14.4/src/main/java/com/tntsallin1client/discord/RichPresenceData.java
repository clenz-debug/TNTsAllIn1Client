package com.tntsallin1client.discord;

import java.util.Objects;

import com.google.gson.JsonObject;

/**
 * What one Discord activity update shows - built anew by {@link DiscordPresenceManager} every time
 * it looks at the game, and compared with the last one sent so that nothing is sent twice.
 */
final class RichPresenceData {
	private static final String LARGE_IMAGE_KEY = "logo";
	private static final String LARGE_IMAGE_TEXT = "TNT's All-In-1 Client";

	/** Top line; null leaves it out. */
	private final String details;
	/** Bottom line; null leaves it out. */
	private final String state;
	/** When the player got to where they are now, for Discord's "elapsed"; null leaves it out. */
	private final Long sessionStartMillis;

	RichPresenceData(String details, String state, Long sessionStartMillis) {
		this.details = details;
		this.state = state;
		this.sessionStartMillis = sessionStartMillis;
	}

	JsonObject toJson() {
		JsonObject activity = new JsonObject();
		if (this.details != null) {
			activity.addProperty("details", this.details);
		}
		if (this.state != null) {
			activity.addProperty("state", this.state);
		}
		if (this.sessionStartMillis != null) {
			JsonObject timestamps = new JsonObject();
			timestamps.addProperty("start", this.sessionStartMillis / 1000L);
			activity.add("timestamps", timestamps);
		}
		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", LARGE_IMAGE_KEY);
		assets.addProperty("large_text", LARGE_IMAGE_TEXT);
		activity.add("assets", assets);
		return activity;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof RichPresenceData)) {
			return false;
		}
		RichPresenceData that = (RichPresenceData) other;
		return Objects.equals(this.details, that.details) && Objects.equals(this.state, that.state)
				&& Objects.equals(this.sessionStartMillis, that.sessionStartMillis);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.details, this.state, this.sessionStartMillis);
	}
}
