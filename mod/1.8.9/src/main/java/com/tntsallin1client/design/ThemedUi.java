package com.tntsallin1client.design;

import com.tntsallin1client.menu.ClientMenuScreen;
import com.tntsallin1client.menu.HudEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

/**
 * Which screens get the client design's look for the game's own buttons, sliders, text fields and
 * backgrounds, as in the Fabric versions: the mod's own option screens and the pause menu - every
 * screen behind the pause menu stays the game's. The widget mixins (`ButtonWidgetMixin` etc.) ask
 * here whether the screen on display is such a one; nothing but the screen on display draws
 * buttons, so the HUD under the pause menu is never touched.
 */
public final class ThemedUi {
	private static final String MENU_PACKAGE = "com.tntsallin1client.menu.";

	private ThemedUi() {
	}

	/**
	 * Client design on, and either one of the mod's own menu screens or the pause menu or title screen
	 * in its client layout. Not the HUD editor (draws the live HUD over the world) and not the list
	 * menu (the Minecraft design's menu).
	 */
	public static boolean isThemed(Screen screen) {
		if (screen == null || !ClientDesign.isClient()) {
			return false;
		}
		if (screen instanceof GameMenuScreen || screen instanceof TitleScreen) {
			return screen instanceof ClientLayoutAccess && ((ClientLayoutAccess) screen).tnt$isClientLayout();
		}
		return screen.getClass().getName().startsWith(MENU_PACKAGE)
				&& !(screen instanceof HudEditorScreen) && !(screen instanceof ClientMenuScreen);
	}

	/** Whether what is being drawn right now belongs to a themed screen. */
	public static boolean active() {
		return isThemed(MinecraftClient.getInstance().currentScreen);
	}

	/** How far the buttons of the screen on display have faded in - they do after a switch of design. */
	public static float widgetAlpha() {
		Screen screen = MinecraftClient.getInstance().currentScreen;
		return screen instanceof GameMenuScreen || screen instanceof TitleScreen ? TitleScreenDesign.clientWidgetAlpha() : 1.0F;
	}

	/** The game's label colors mapped onto the theme: white becomes the text color, gray a dimmed text color. */
	public static int textColor(int color) {
		int rgb = color & 0xFFFFFF;
		ClientTheme theme = ClientTheme.get();
		if (rgb == 0xFFFFFF) {
			return theme.text;
		}
		if (rgb == 0xA0A0A0 || rgb == 0xAAAAAA || rgb == 0x808080 || rgb == 0x707070) {
			return ClientTheme.withAlpha(theme.text, 0.6F);
		}
		return 0xFF000000 | rgb;
	}
}
