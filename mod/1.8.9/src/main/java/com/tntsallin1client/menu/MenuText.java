package com.tntsallin1client.menu;

import net.minecraft.client.resource.language.I18n;

/** Text helpers shared by the mod's screens. */
public final class MenuText {
	private MenuText() {
	}

	/** The label of an on/off button, e.g. "FPS Counter: ON" - the same wording vanilla's own option buttons use. */
	public static String onOff(String labelKey, boolean on) {
		return I18n.translate(labelKey) + ": " + I18n.translate(on ? "options.on" : "options.off");
	}
}
