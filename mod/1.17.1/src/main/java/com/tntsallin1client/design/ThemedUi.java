package com.tntsallin1client.design;

import com.tntsallin1client.menu.ClientMenuScreen;
import com.tntsallin1client.menu.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jetbrains.annotations.Nullable;

/**
 * Which screens get the client design's look for vanilla widgets (own user request: the mod's own
 * option screens and the pause menu in the client design, everything behind the pause menu stays
 * vanilla). The widget mixins ({@code AbstractButtonThemeMixin} etc.) restyle buttons, sliders, edit
 * boxes and lists only while such a screen is being drawn - {@link #begin}/{@link #end} around the
 * screen's render, so the in-game HUD drawn underneath the pause menu is never touched.
 */
public final class ThemedUi {
	private static final String MENU_PACKAGE = "com.tntsallin1client.menu.";

	private static boolean drawing;
	private static boolean active;
	private static boolean ownScreen;

	private ThemedUi() {
	}

	/**
	 * Client design on, and either one of the mod's own menu screens or the pause menu or title screen
	 * in its client layout (the title screen's own buttons draw themselves, this is for the vanilla
	 * buttons added to it: the friends button). Not the HUD editor (draws the live HUD over the world)
	 * and not the Minecraft-design list menu (only ever opened in the Minecraft design anyway).
	 */
	public static boolean isThemed(@Nullable Screen screen) {
		if (screen == null || !ClientDesign.isClient()) {
			return false;
		}
		if (screen instanceof PauseScreen) {
			return screen instanceof PauseScreenLayoutAccess access && access.tntsallin1client$isClientLayout();
		}
		if (screen instanceof TitleScreen) {
			return screen instanceof TitleScreenLayoutAccess access && access.tntsallin1client$isClientLayout();
		}
		return screen.getClass().getName().startsWith(MENU_PACKAGE)
				&& !(screen instanceof HudEditorScreen) && !(screen instanceof ClientMenuScreen);
	}

	public static void begin(Screen screen) {
		drawing = true;
		active = isThemed(screen);
		ownScreen = screen.getClass().getName().startsWith(MENU_PACKAGE);
	}

	public static void end() {
		drawing = false;
		active = false;
		ownScreen = false;
	}

	/** Whether the widget being drawn right now belongs to one of the mod's own screens, in either design. */
	public static boolean ownScreen() {
		return ownScreen;
	}

	/** Whether the widget being drawn right now belongs to a themed screen. */
	public static boolean active() {
		return active;
	}

	/**
	 * Whether text fields measure and draw their text in the client font right now: while a screen is
	 * drawn, whether that screen is themed; otherwise - typing and clicks, which move the cursor and
	 * scroll the text outside of drawing - whether the open screen is.
	 */
	public static boolean clientFontFields() {
		return drawing ? active : isThemed(Minecraft.getInstance().screen);
	}

	/**
	 * A text field's colors mapped onto the theme like {@link #textColor}: the text color of an
	 * editable field (0xE0E0E0) becomes the text color, that of a field that can't be edited (0x707070)
	 * a dimmed text color.
	 */
	public static int fieldTextColor(int color) {
		int rgb = color & 0xFFFFFF;
		if (rgb == 0xE0E0E0) {
			return textColor(color | 0xFFFFFF);
		}
		if (rgb == 0x707070) {
			return textColor((color & 0xFF000000) | 0xA0A0A0);
		}
		return textColor(color);
	}

	/** Vanilla label colors mapped onto the theme: white becomes the text color, gray a dimmed text color. */
	public static int textColor(int color) {
		int rgb = color & 0xFFFFFF;
		ClientTheme theme = ClientTheme.get();
		if (rgb == 0xFFFFFF) {
			return ClientTheme.withAlpha(theme.text, ((color >>> 24) & 0xFF) / 255.0f);
		}
		if (rgb == 0xA0A0A0 || rgb == 0xAAAAAA || rgb == 0x808080) {
			return ClientTheme.withAlpha(theme.text, 0.6f);
		}
		return color;
	}
}
