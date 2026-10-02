package com.tntsallin1client.discord;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

/**
 * What one Discord Rich Presence update should show - built fresh by
 * {@link DiscordPresenceManager} every time it re-evaluates the current game state, compared
 * against the last one actually sent so an unchanged state never re-sends (see
 * {@link DiscordIpcClient#setActivity}'s own doc comment for why that matters: nothing reads
 * Discord's per-update acks back, so needless resends would just accumulate as unread noise on the
 * connection for the rest of the session).
 *
 * @param details            top line - {@code null} omits it entirely rather than showing a blank line.
 * @param state              bottom line - {@code null} same as above.
 * @param sessionStartMillis {@code System.currentTimeMillis()} of when the current scope (main menu /
 *                           this singleplayer world / this server) was entered - shown by Discord as
 *                           "00:15 elapsed", reset by {@link DiscordPresenceManager} on every scope
 *                           change so it always reflects time in the *current* place, not since the
 *                           game itself launched. {@code null} when the elapsed-time toggle is off.
 */
public record RichPresenceData(@Nullable String details, @Nullable String state, @Nullable Long sessionStartMillis) {
	static final String LARGE_IMAGE_KEY = "logo";
	static final String LARGE_IMAGE_TEXT = "TNT's All-In-1 Client";

	JsonObject toJson() {
		JsonObject activity = new JsonObject();
		if (details != null) activity.addProperty("details", details);
		if (state != null) activity.addProperty("state", state);
		if (sessionStartMillis != null) {
			JsonObject timestamps = new JsonObject();
			timestamps.addProperty("start", sessionStartMillis / 1000L);
			activity.add("timestamps", timestamps);
		}
		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", LARGE_IMAGE_KEY);
		assets.addProperty("large_text", LARGE_IMAGE_TEXT);
		activity.add("assets", assets);
		return activity;
	}
}
