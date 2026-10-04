package com.tntsallin1client.menu;

import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.design.ClientDesign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * The buttons an options screen has besides its options: "Back"; "Move/Resize" (the HUD editor) on
 * the screens of features that put something on the HUD - reachable right from their options and
 * not only from the menu, but not offered where there is nothing to move (both own user requests);
 * and, on a feature's own screen, "Reset" ({@link ResetButtons}). Each options screen calls
 * {@link #add} at the end of its layout, where it used to add its "Back" button itself.
 *
 * <p>Where they go depends on the design (own user request):
 * <ul>
 * <li>Minecraft design: "Move/Resize" continues the screen's column of options, "Back" comes
 * directly below it; "Reset" sits right of the options, level with the first one.
 * <li>Client design: like the card menu's top bar - "Back" in the top left corner, "Move/Resize" in
 * the top right one, "Reset" below that. Nothing is added to the column.
 * </ul>
 *
 * <p>The top bar lies above the area the scrolling options screens clip their widgets to, so its
 * buttons are not handed to the screen for drawing: the screen only gets them for clicks, and
 * {@link #renderTopBar} draws them after the screen is done ({@code ScreenThemeMixin}).
 */
public final class OptionsChrome {
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	// The client design's top bar - the same measures as ClientModsCardScreen's.
	private static final int MARGIN = 10;
	private static final int TOP_BAR_Y = 8;
	private static final int TOP_BAR_BUTTON_WIDTH = 80;
	private static final int SECOND_ROW_Y = TOP_BAR_Y + BUTTON_HEIGHT + 4;
	// "Reset" in the Minecraft design. Its height on screen is where the options screens put their
	// first row - and where the scrolling ones start drawing, so it is not cut away.
	private static final int RESET_Y = 40;
	private static final int RESET_WIDTH = 84;
	private static final int RESET_MARGIN = 6;
	/** The options are a column this wide in the middle; the scrolling screens put their scrollbar right of it. */
	private static final int OPTIONS_WIDTH = 210;
	private static final int SCROLLBAR_SPACE = 16;
	/** Shown instead of the word where a narrow window leaves no room for it. */
	private static final String SHORT_RESET_LABEL = "↺";

	/** The top bar buttons of each screen laid out in the client design. */
	private static final Map<Screen, List<Button>> TOP_BARS = new WeakHashMap<>();

	private OptionsChrome() {
	}

	/**
	 * @param x          left edge of the screen's column of options
	 * @param y          where that column continues
	 * @param width      the column's width
	 * @param back       what "Back" does
	 * @param drawn      the screen's {@code addRenderableWidget}
	 * @param clickOnly  the screen's {@code addWidget}
	 */
	public static void add(Screen screen, int x, int y, int width, Runnable back, Consumer<Button> drawn, Consumer<Button> clickOnly) {
		Minecraft client = Minecraft.getInstance();
		Runnable openHudEditor = () -> client.setScreen(new HudEditorScreen(screen));
		ConfigReset.Feature feature = ConfigReset.Feature.of(screen);
		boolean hudEditor = hasHudElement(screen.getClass());

		if (ClientDesign.isClient()) {
			int right = screen.width - MARGIN - TOP_BAR_BUTTON_WIDTH;
			List<Button> topBar = new ArrayList<>();
			topBar.add(button(CommonComponents.GUI_BACK, back, MARGIN, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH));
			if (hudEditor) {
				topBar.add(button(Component.translatable("gui.tntsallin1client.cards.move_resize"), openHudEditor, right, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH));
			}
			if (feature != null) {
				// Below "Move/Resize" - or in its place where there is none.
				topBar.add(button(Component.translatable("gui.tntsallin1client.reset.button"), ResetButtons.ask(client, screen, feature),
						right, hudEditor ? SECOND_ROW_Y : TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH));
			}
			topBar.forEach(clickOnly);
			TOP_BARS.put(screen, topBar);
			return;
		}

		TOP_BARS.remove(screen);
		if (hudEditor) {
			drawn.accept(button(Component.translatable("gui.tntsallin1client.menu.hud_editor_button"), openHudEditor, x, y, width));
			y += ROW_SPACING;
		}
		drawn.accept(button(CommonComponents.GUI_BACK, back, x, y, width));
		if (feature != null) {
			Component label = Component.translatable("gui.tntsallin1client.reset.button");
			boolean fits = (screen.width - OPTIONS_WIDTH) / 2 - SCROLLBAR_SPACE - RESET_MARGIN >= RESET_WIDTH;
			int resetWidth = fits ? RESET_WIDTH : BUTTON_HEIGHT;
			Button reset = button(fits ? label : Component.literal(SHORT_RESET_LABEL), ResetButtons.ask(client, screen, feature),
					screen.width - RESET_MARGIN - resetWidth, RESET_Y, resetWidth);
			if (!fits) {
				reset.setTooltip(Tooltip.create(label));
			}
			drawn.accept(reset);
		}
	}

	/**
	 * How much of the screen's column {@link #add} takes up - for the scrolling screens, which need
	 * their content's height before laying it out.
	 */
	public static int flowHeight(Class<? extends Screen> screen) {
		if (ClientDesign.isClient()) {
			return 0;
		}
		return hasHudElement(screen) ? ROW_SPACING + BUTTON_HEIGHT : BUTTON_HEIGHT;
	}

	/** Whether the screen belongs to a feature with something on the HUD - its own screen or one of its sub-screens. */
	private static boolean hasHudElement(Class<? extends Screen> screen) {
		if (screen == KeystrokesKeysOptionsScreen.class) {
			return ConfigReset.Feature.KEYSTROKES.hasHudElement();
		}
		if (screen == ArmorStatusSlotsOptionsScreen.class) {
			return ConfigReset.Feature.ARMOR_STATUS.hasHudElement();
		}
		for (ConfigReset.Feature feature : ConfigReset.Feature.values()) {
			if (feature.optionsScreen == screen) {
				return feature.hasHudElement();
			}
		}
		return false;
	}

	/** Draws the screen's top bar, if it has one. Called once the screen has drawn itself. */
	public static void renderTopBar(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		for (Button button : TOP_BARS.getOrDefault(screen, Collections.emptyList())) {
			button.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}
	}

	/**
	 * "Back" for a screen that lays everything else out itself (credits, friends, waypoint list): in
	 * the client design in the top left corner like on the options screens (own user request), else
	 * where the screen wants it.
	 */
	public static Button back(Runnable onPress, int x, int y, int width) {
		return ClientDesign.isClient()
				? button(CommonComponents.GUI_BACK, onPress, MARGIN, TOP_BAR_Y, TOP_BAR_BUTTON_WIDTH)
				: button(CommonComponents.GUI_BACK, onPress, x, y, width);
	}

	private static Button button(Component label, Runnable onPress, int x, int y, int width) {
		return Button.builder(label, pressed -> onPress.run()).bounds(x, y, width, BUTTON_HEIGHT).build();
	}
}
