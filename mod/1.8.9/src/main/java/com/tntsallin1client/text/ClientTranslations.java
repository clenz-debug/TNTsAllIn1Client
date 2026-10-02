package com.tntsallin1client.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Our texts for the game's own translation table. Without a mod loader our jar isn't a resource
 * pack the game would read language files from, so they are added to the table directly whenever
 * the game (re)loads its languages (see `TranslationStorageMixin`) - after that
 * {@code I18n.translate} knows our keys like any vanilla one, the Controls screen included.
 */
public final class ClientTranslations {
	private ClientTranslations() {
	}

	/**
	 * Adds `assets/tntsallin1client/lang/<languageCode>.lang` to `translations`. Does nothing for a
	 * language we have no file for - the game loads English first, so that is what stays then.
	 */
	public static void addTo(Map<String, String> translations, String languageCode) {
		InputStream stream = ClientTranslations.class.getResourceAsStream("/assets/tntsallin1client/lang/" + languageCode + ".lang");
		if (stream == null) {
			return;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int separator = line.indexOf('=');
				if (separator <= 0 || line.charAt(0) == '#') {
					continue;
				}
				translations.put(line.substring(0, separator), line.substring(separator + 1));
			}
		} catch (IOException e) {
			// A language file of ours that can't be read just leaves the raw keys showing.
		}
	}
}
