package com.tntsallin1client.tour;

import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.design.TitleScreenLayoutAccess;
import com.tntsallin1client.friends.FriendsBridge;
import com.tntsallin1client.menu.ClientMenuScreen;
import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.menu.ClientModsCardScreen;
import com.tntsallin1client.menu.FpsCounterOptionsScreen;
import com.tntsallin1client.menu.FriendsInGameScreen;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The in-game tour's steps (own user request), in order: on the title screen the Client Mods menu
 * in the design the player picked, the key binding hint, the Friends button, then the same in the
 * other design (only looking - nothing can be switched on there); then the player creates a world,
 * opens the pause menu and tries things out by clicking themselves: the design switch, the Client
 * Mods menu, one feature, its options, and the friends screen. Which design to keep is asked at the
 * very end ({@code TourFinishScreen}).
 */
public final class InGameTourSteps {
	private static final String OPEN_MODS = "gui.tntsallin1client.menu.open_button";
	private static final String DESIGN_BUTTON = "gui.tntsallin1client.design.button";
	private static final String LOGO = "gui.tntsallin1client.design.switch";
	private static final String FPS = "gui.tntsallin1client.menu.fps_counter";
	private static final String[] FRIENDS_BUTTON = {"gui.tntsallin1client.friends.title", "gui.tntsallin1client.friends.button_with_invites"};

	private InGameTourSteps() {
	}

	/**
	 * @param startClient  the design the player had when the tour started - shown first
	 * @param bothDesigns  false while Essential locks the design switch
	 */
	public static List<TourStep> build(boolean startClient, boolean bothDesigns) {
		List<TourStep> steps = new ArrayList<>();
		steps.add(info("welcome", InGameTourSteps::isTitle, TourStep.Target.NONE, null));

		addTitleDesign(steps, startClient, true, bothDesigns);
		if (bothDesigns) {
			addTitleDesign(steps, !startClient, false, true);
		}

		// Out of the title screen and into a world - on their own, the hint just points the way.
		steps.add(new TourStep("create_world", TourStep.Kind.WAIT, screen -> Minecraft.getInstance().level == null,
				widget("menu.singleplayer", "selectWorld.create"), null,
				mc -> mc.level != null && mc.player != null, () -> true));
		steps.add(new TourStep("open_pause", TourStep.Kind.WAIT, screen -> screen == null && Minecraft.getInstance().level != null,
				TourStep.Target.NONE, null, mc -> isPause(mc.screen), () -> true));

		// The pause menu shows the design the title screen part ended on - the other one than at the start.
		boolean pauseClient = bothDesigns != startClient;
		String pauseDesign = pauseClient ? "client" : "minecraft";
		steps.add(info("pause_intro_" + pauseDesign, InGameTourSteps::isPause,
				union(widget(OPEN_MODS), widget(DESIGN_BUTTON, LOGO)), null));
		if (bothDesigns) {
			steps.add(action("pause_switch_" + pauseDesign, InGameTourSteps::isPause, widget(DESIGN_BUTTON, LOGO),
					mc -> ClientDesign.isClient() != InGameTour.designAtStepStart() && !TitleScreenDesign.isRunning() && isPause(mc.screen)));
		}
		steps.add(action("pause_mods", InGameTourSteps::isPause, widget(OPEN_MODS), mc -> isModMenu(mc.screen)));

		steps.add(new TourStep("menu_try", TourStep.Kind.TRY, InGameTourSteps::isModMenu,
				menuTarget(TourTargets.FEATURE_SWITCH + FPS), null, null, () -> true));
		steps.add(action("menu_options", InGameTourSteps::isModMenu, menuTarget(TourTargets.FEATURE_OPTIONS + FPS),
				mc -> mc.screen instanceof FpsCounterOptionsScreen));
		steps.add(info("options_keys", screen -> screen instanceof FpsCounterOptionsScreen, InGameTourSteps::allWidgets,
				InGameTourSteps::closeScreen));
		steps.add(info("menu_hud", InGameTourSteps::isModMenu, menuTarget(TourTargets.HUD_EDITOR), null));
		steps.add(action("menu_done", InGameTourSteps::isModMenu, widget("gui.done"), mc -> isPause(mc.screen)));

		BooleanSupplier friends = FriendsBridge::isActive;
		steps.add(new TourStep("pause_friends", TourStep.Kind.ACTION, InGameTourSteps::isPause, widget(FRIENDS_BUTTON), null,
				mc -> mc.screen instanceof FriendsInGameScreen, friends));
		steps.add(new TourStep("friends", TourStep.Kind.INFO, screen -> screen instanceof FriendsInGameScreen, TourStep.Target.NONE,
				InGameTourSteps::closeScreen, null, friends));
		return steps;
	}

	/** The title screen part for one design: its Client Mods menu (look only), then the design switch. */
	private static void addTitleDesign(List<TourStep> steps, boolean client, boolean first, boolean bothDesigns) {
		Predicate<@Nullable Screen> title = client ? InGameTourSteps::isClientTitle : InGameTourSteps::isMinecraftTitle;
		steps.add(info(first ? "title_mods" : "title_mods_again", title, widget(OPEN_MODS),
				mc -> mc.setScreen(ClientMenus.create(mc.screen))));
		if (client) {
			Predicate<@Nullable Screen> cards = screen -> screen instanceof ClientModsCardScreen;
			steps.add(info("cards_card", cards, menuTarget(TourTargets.FEATURE + FPS), null));
			steps.add(info("cards_top", cards, menuTarget(TourTargets.TOP_BAR), InGameTourSteps::closeScreen));
		} else {
			Predicate<@Nullable Screen> list = screen -> screen instanceof ClientMenuScreen;
			steps.add(info("list_features", list, menuTarget(TourTargets.FEATURE + FPS), null));
			steps.add(info("list_search", list, menuTarget(TourTargets.SEARCH), InGameTourSteps::closeScreen));
		}
		if (first) {
			steps.add(info("title_options", title, widget("menu.options"), null));
			// The Friends button is on the title screen too (own user request: the tour should point it
			// out) - like the pause menu's, only with friends available at all.
			steps.add(new TourStep("title_friends", TourStep.Kind.INFO, title, widget(FRIENDS_BUTTON), null, null, FriendsBridge::isActive));
		}
		if (!bothDesigns) {
			return;
		}
		TourStep.Target designSwitch = client ? widget(LOGO) : widget(DESIGN_BUTTON);
		if (first) {
			steps.add(info(client ? "title_switch_to_minecraft" : "title_switch_to_client", title, designSwitch,
					mc -> switchTitleDesign(mc, !client)));
		} else {
			steps.add(info("title_switch_back_" + (client ? "client" : "minecraft"), title, designSwitch, null));
		}
	}

	// --- Step factories ----------------------------------------------------------------------

	private static TourStep info(String id, Predicate<@Nullable Screen> screen, TourStep.Target target, @Nullable Consumer<Minecraft> onNext) {
		return new TourStep(id, TourStep.Kind.INFO, screen, target, onNext, null, () -> true);
	}

	private static TourStep action(String id, Predicate<@Nullable Screen> screen, TourStep.Target target, Predicate<Minecraft> done) {
		return new TourStep(id, TourStep.Kind.ACTION, screen, target, null, done, () -> true);
	}

	// --- Screens -----------------------------------------------------------------------------

	static boolean isTitle(@Nullable Screen screen) {
		return screen instanceof TitleScreen;
	}

	static boolean isClientTitle(@Nullable Screen screen) {
		return screen instanceof TitleScreen && screen instanceof TitleScreenLayoutAccess access && access.tntsallin1client$isClientLayout();
	}

	static boolean isMinecraftTitle(@Nullable Screen screen) {
		return screen instanceof TitleScreen && !isClientTitle(screen);
	}

	static boolean isPause(@Nullable Screen screen) {
		return screen instanceof PauseScreen pause && pause.showsPauseMenu();
	}

	static boolean isModMenu(@Nullable Screen screen) {
		return screen instanceof ClientMenuScreen || screen instanceof ClientModsCardScreen;
	}

	// --- Targets -----------------------------------------------------------------------------

	/** The first visible widget labeled with one of these translation keys. */
	private static TourStep.Target widget(String... keys) {
		return screen -> {
			if (screen == null) return null;
			for (AbstractWidget widget : Screens.getWidgets(screen)) {
				if (widget.visible && hasKey(widget.getMessage(), keys)) {
					return TourRect.of(widget);
				}
			}
			return null;
		};
	}

	private static boolean hasKey(Component message, String[] keys) {
		if (!(message.getContents() instanceof TranslatableContents translatable)) return false;
		for (String key : keys) {
			if (key.equals(translatable.getKey())) return true;
		}
		return false;
	}

	private static TourStep.Target union(TourStep.Target a, TourStep.Target b) {
		return screen -> TourRect.union(a.resolve(screen), b.resolve(screen));
	}

	private static TourStep.Target menuTarget(String name) {
		return screen -> screen instanceof TourTargets targets ? targets.tourTarget(name) : null;
	}

	/** Everything on an options screen - the part below its title. */
	private static @Nullable TourRect allWidgets(@Nullable Screen screen) {
		if (screen == null) return null;
		TourRect all = null;
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget.visible) all = TourRect.union(all, TourRect.of(widget));
		}
		return all;
	}

	// --- Actions -----------------------------------------------------------------------------

	private static void closeScreen(Minecraft minecraft) {
		if (minecraft.screen != null) {
			minecraft.screen.onClose();
		}
	}

	/** The title screen's own design switch, with its animation - same as clicking it. */
	private static void switchTitleDesign(Minecraft minecraft, boolean toClient) {
		Screen screen = minecraft.screen;
		if (screen == null) return;
		if (toClient) {
			TitleScreenDesign.switchToClient();
		} else {
			TitleScreenDesign.switchToMinecraft(TitleScreenDesign.clientLogoRect(screen.width, screen.height),
					() -> screen.resize(screen.width, screen.height));
		}
	}
}
