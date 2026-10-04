package com.tntsallin1client.tour;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.ClientLayoutAccess;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.friends.FriendsBridge;
import com.tntsallin1client.menu.ClientMenuScreen;
import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.menu.ClientModsCardScreen;
import com.tntsallin1client.menu.FpsCounterOptionsScreen;
import com.tntsallin1client.menu.FriendsInGameScreen;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * The in-game tour's steps, in the Fabric versions' order: on the title screen the Client Mods menu
 * in the design the player picked, the key binding hint, the Friends button, then the same in the
 * other design (only looking - nothing can be switched on there); then the player creates a world,
 * opens the pause menu and tries things out by clicking themselves: the design switch, the Client
 * Mods menu, one feature, its options, and the friends screen. Which design to keep is asked at the
 * very end (`TourFinishScreen`).
 */
public final class InGameTourSteps {
	private static final String FPS = "gui.tntsallin1client.menu.fps_counter";
	/** The column of options on an options screen - its rows are not buttons the screen hands out. */
	private static final int OPTIONS_WIDTH = 210;
	private static final int OPTIONS_TOP = 40;
	private static final int OPTIONS_HEIGHT = 120;

	private static final BooleanSupplier ALWAYS = () -> true;

	private InGameTourSteps() {
	}

	/** `startClient`: the design the player had when the tour started - shown first. */
	public static List<TourStep> build(boolean startClient) {
		List<TourStep> steps = new ArrayList<TourStep>();
		steps.add(info("welcome", InGameTourSteps::isTitle, TourStep.Target.NONE, null));

		addTitleDesign(steps, startClient, true);
		addTitleDesign(steps, !startClient, false);

		// Out of the title screen and into a world - on their own, the hint just points the way.
		steps.add(new TourStep("create_world", TourStep.Kind.WAIT, screen -> MinecraftClient.getInstance().world == null,
				labeled("menu.singleplayer", "selectWorld.create"), null,
				client -> client.world != null && client.player != null, ALWAYS));
		steps.add(new TourStep("open_pause", TourStep.Kind.WAIT, screen -> screen == null && MinecraftClient.getInstance().world != null,
				TourStep.Target.NONE, null, client -> isPause(client.currentScreen), ALWAYS));

		// The pause menu shows the design the title screen part ended on - the other one than at the start.
		String pauseDesign = startClient ? "minecraft" : "client";
		TourStep.Target openMods = button(MenuButtons.OPEN_MENU_BUTTON_ID);
		TourStep.Target designSwitch = button(MenuButtons.DESIGN_BUTTON_ID);
		steps.add(info("pause_intro_" + pauseDesign, InGameTourSteps::isPause, union(openMods, designSwitch), null));
		steps.add(action("pause_switch_" + pauseDesign, InGameTourSteps::isPause, designSwitch,
				client -> ClientDesign.isClient() != InGameTour.designAtStepStart() && !TitleScreenDesign.isRunning() && isPause(client.currentScreen)));
		steps.add(action("pause_mods", InGameTourSteps::isPause, openMods, client -> isModMenu(client.currentScreen)));

		steps.add(new TourStep("menu_try", TourStep.Kind.TRY, InGameTourSteps::isModMenu,
				menuTarget(TourTargets.FEATURE_SWITCH + FPS), null, null, ALWAYS));
		steps.add(action("menu_options", InGameTourSteps::isModMenu, menuTarget(TourTargets.FEATURE_OPTIONS + FPS),
				client -> client.currentScreen instanceof FpsCounterOptionsScreen));
		steps.add(info("options_keys", screen -> screen instanceof FpsCounterOptionsScreen, InGameTourSteps::optionsColumn,
				InGameTourSteps::closeScreen));
		steps.add(info("menu_hud", InGameTourSteps::isModMenu, menuTarget(TourTargets.HUD_EDITOR), null));
		steps.add(action("menu_done", InGameTourSteps::isModMenu, labeled("gui.done"), client -> isPause(client.currentScreen)));

		BooleanSupplier friends = FriendsBridge::isActive;
		steps.add(new TourStep("pause_friends", TourStep.Kind.ACTION, InGameTourSteps::isPause, button(MenuButtons.FRIENDS_BUTTON_ID), null,
				client -> client.currentScreen instanceof FriendsInGameScreen, friends));
		steps.add(new TourStep("friends", TourStep.Kind.INFO, screen -> screen instanceof FriendsInGameScreen, TourStep.Target.NONE,
				InGameTourSteps::closeScreen, null, friends));
		return steps;
	}

	/** The title screen part for one design: its Client Mods menu (look only), then the design switch. */
	private static void addTitleDesign(List<TourStep> steps, final boolean client, boolean first) {
		Predicate<Screen> title = client ? InGameTourSteps::isClientTitle : InGameTourSteps::isMinecraftTitle;
		steps.add(info(first ? "title_mods" : "title_mods_again", title, button(MenuButtons.OPEN_MENU_BUTTON_ID),
				mc -> mc.setScreen(ClientMenus.create(mc.currentScreen))));
		if (client) {
			Predicate<Screen> cards = screen -> screen instanceof ClientModsCardScreen;
			steps.add(info("cards_card", cards, menuTarget(TourTargets.FEATURE + FPS), null));
			steps.add(info("cards_top", cards, menuTarget(TourTargets.TOP_BAR), InGameTourSteps::closeScreen));
		} else {
			Predicate<Screen> list = screen -> screen instanceof ClientMenuScreen;
			steps.add(info("list_features", list, menuTarget(TourTargets.FEATURE + FPS), null));
			steps.add(info("list_search", list, menuTarget(TourTargets.SEARCH), InGameTourSteps::closeScreen));
		}
		if (first) {
			steps.add(info("title_options", title, labeled("menu.options"), null));
			// The Friends button is on the title screen too - like the pause menu's, only with friends available at all.
			steps.add(new TourStep("title_friends", TourStep.Kind.INFO, title, button(MenuButtons.FRIENDS_BUTTON_ID), null, null, FriendsBridge::isActive));
		}
		TourStep.Target designSwitch = button(MenuButtons.DESIGN_BUTTON_ID);
		if (first) {
			steps.add(info(client ? "title_switch_to_minecraft" : "title_switch_to_client", title, designSwitch,
					mc -> switchTitleDesign(mc, !client)));
		} else {
			steps.add(info("title_switch_back_" + (client ? "client" : "minecraft"), title, designSwitch, null));
		}
	}

	// --- Step factories ----------------------------------------------------------------------

	private static TourStep info(String id, Predicate<Screen> screen, TourStep.Target target, Consumer<MinecraftClient> onNext) {
		return new TourStep(id, TourStep.Kind.INFO, screen, target, onNext, null, ALWAYS);
	}

	private static TourStep action(String id, Predicate<Screen> screen, TourStep.Target target, Predicate<MinecraftClient> done) {
		return new TourStep(id, TourStep.Kind.ACTION, screen, target, null, done, ALWAYS);
	}

	// --- Screens -----------------------------------------------------------------------------

	static boolean isTitle(Screen screen) {
		return screen instanceof TitleScreen;
	}

	static boolean isClientTitle(Screen screen) {
		return screen instanceof TitleScreen && screen instanceof ClientLayoutAccess && ((ClientLayoutAccess) screen).tnt$isClientLayout();
	}

	static boolean isMinecraftTitle(Screen screen) {
		return screen instanceof TitleScreen && !isClientTitle(screen);
	}

	static boolean isPause(Screen screen) {
		return screen instanceof GameMenuScreen;
	}

	static boolean isModMenu(Screen screen) {
		return screen instanceof ClientMenuScreen || screen instanceof ClientModsCardScreen;
	}

	// --- Targets -----------------------------------------------------------------------------

	private static List<ButtonWidget> buttons(Screen screen) {
		return ((TourScreen) screen).tnt$buttons();
	}

	/** The first visible button with that id - the mod's own buttons, which have ids no other button has. */
	private static TourStep.Target button(final int id) {
		return screen -> {
			if (screen == null) {
				return null;
			}
			for (ButtonWidget button : buttons(screen)) {
				if (button.visible && button.id == id) {
					return TourRect.of(button);
				}
			}
			return null;
		};
	}

	/** The first visible button that says what one of these translation keys says. */
	private static TourStep.Target labeled(final String... keys) {
		return screen -> {
			if (screen == null) {
				return null;
			}
			for (ButtonWidget button : buttons(screen)) {
				for (String key : keys) {
					if (button.visible && I18n.translate(key).equals(button.message)) {
						return TourRect.of(button);
					}
				}
			}
			return null;
		};
	}

	private static TourStep.Target union(final TourStep.Target a, final TourStep.Target b) {
		return screen -> TourRect.union(a.resolve(screen), b.resolve(screen));
	}

	private static TourStep.Target menuTarget(final String name) {
		return screen -> screen instanceof TourTargets ? ((TourTargets) screen).tourTarget(name) : null;
	}

	/** Everything on an options screen - the part below its title. */
	private static TourRect optionsColumn(Screen screen) {
		if (screen == null) {
			return null;
		}
		return new TourRect((screen.width - OPTIONS_WIDTH) / 2, OPTIONS_TOP, OPTIONS_WIDTH, Math.min(OPTIONS_HEIGHT, screen.height - OPTIONS_TOP - 10));
	}

	// --- Actions -----------------------------------------------------------------------------

	private static void closeScreen(MinecraftClient client) {
		if (client.currentScreen instanceof TourScreen.Closable) {
			((TourScreen.Closable) client.currentScreen).closeForTour();
		}
	}

	/** The title screen's own design switch, with its animation - same as clicking it. */
	private static void switchTitleDesign(MinecraftClient client, boolean toClient) {
		Screen screen = client.currentScreen;
		if (screen == null) {
			return;
		}
		if (toClient) {
			TitleScreenDesign.switchToClient();
		} else {
			TitleScreenDesign.switchToMinecraft(TitleScreenDesign.clientLogoRect(screen.width, screen.height));
			// The game's own buttons are built right away, under the animation.
			screen.init(client, screen.width, screen.height);
		}
	}
}
