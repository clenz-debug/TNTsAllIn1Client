package com.tntsallin1client.design;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * The client design's title screen, as in the other versions (client logo on a plain theme-colored
 * background, Singleplayer/Multiplayer/Realms/Options/Client Mods below it, quit as an X in the top
 * right) plus the animated switch between that and the game's own title screen. The game's
 * `TitleScreen` stays the screen object either way - `TitleScreenMixin` rearranges what its `init`
 * built and swaps what its `render` does.
 *
 * <p>Singleplayer, Multiplayer, Realms and Options are the game's own buttons, moved into the
 * column and drawn in the theme ({@link ThemedUi}) - so the game itself acts on a click, exactly as
 * it does in its own layout. They are found by their captions.
 *
 * <p>The switch animation: the theme background fades over (or away from) the game's title screen
 * while the logo grows out of that layout's Client Design button into its big spot in the client
 * layout (or shrinks back into it).
 */
public final class TitleScreenDesign {
	private static final long TRANSITION_MS = 800;
	private static final long BUTTON_FADE_MS = 300;
	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 6;
	private static final int BUTTON_COUNT = 5;
	private static final int LOGO_GAP = 14;
	private static final int QUIT_SIZE = 20;
	/** The game's captions of the buttons the client layout keeps, top to bottom. */
	private static final String[] KEPT_BUTTONS = {"menu.singleplayer", "menu.multiplayer", "menu.online", "menu.options"};
	/** Logo height at the game's own layout's end of the animation - small enough to sit on the Client Design button. */
	private static final float ANCHOR_LOGO_HEIGHT = 16;

	private static long transitionStart = -1;
	private static boolean transitionToClient;
	private static Rect transitionFrom;
	private static long buttonFadeStart = -1;
	/** Where the logo starts from / shrinks back into in the game's own layout: centered on its Client Design button. */
	private static Rect minecraftAnchor;

	private TitleScreenDesign() {
	}

	/** A logo position: top-left corner plus height (width follows from {@link ClientLogo#widthFor}). */
	public static final class Rect {
		public final float x;
		public final float y;
		public final float height;

		Rect(float x, float y, float height) {
			this.x = x;
			this.y = y;
			this.height = height;
		}

		public static Rect centered(float centerX, float top, float height) {
			return new Rect(centerX - ClientLogo.widthFor(height) / 2, top, height);
		}

		Rect lerp(Rect to, float t) {
			return new Rect(this.x + (to.x - this.x) * t, this.y + (to.y - this.y) * t, this.height + (to.height - this.height) * t);
		}
	}

	/** Whether `init` should build the client layout right now - not while the switch towards it is still animating over the game's own one. */
	public static boolean useClientLayout() {
		return ClientDesign.isClient() && !(isRunning() && transitionToClient) && !Minecraft.getInstance().isDemo();
	}

	// --- Layout -------------------------------------------------------------------------------

	/** Called by `MenuButtons` whenever it places the game's own layout's Client Design button. */
	public static void setMinecraftAnchor(int x, int y, int width, int height) {
		minecraftAnchor = Rect.centered(x + width / 2.0F, y + (height - ANCHOR_LOGO_HEIGHT) / 2, ANCHOR_LOGO_HEIGHT);
	}

	private static Rect minecraftAnchor(int width, int height) {
		return minecraftAnchor != null ? minecraftAnchor : Rect.centered(width / 2.0F, height / 2.0F, ANCHOR_LOGO_HEIGHT);
	}

	/** The big logo of the client layout, sized to whatever room the button column leaves. */
	public static Rect clientLogoRect(int width, int height) {
		int buttonsHeight = BUTTON_COUNT * BUTTON_HEIGHT + (BUTTON_COUNT - 1) * BUTTON_GAP;
		float logoHeight = Math.max(40, Math.min(150, height - buttonsHeight - LOGO_GAP - 40));
		float top = Math.max(8, (height - logoHeight - LOGO_GAP - buttonsHeight) / 2);
		return Rect.centered(width / 2.0F, top, logoHeight);
	}

	/**
	 * Turns what the game's own `init` built into the client layout. `vanilla` are the buttons it
	 * added; `clear` empties the screen of them, `add` puts a button onto the screen. `onLogo` is what
	 * a click on the logo does.
	 */
	public static void buildClientLayout(Screen screen, List<AbstractWidget> vanilla, Runnable clear, Consumer<AbstractWidget> add, Runnable onLogo) {
		List<AbstractWidget> before = new ArrayList<AbstractWidget>(vanilla);
		clear.run();
		int width = screen.width;
		Rect logo = clientLogoRect(width, screen.height);
		add.accept(new LogoButton(Math.round(logo.x), Math.round(logo.y), Math.round(logo.height), onLogo));

		int x = (width - BUTTON_WIDTH) / 2;
		int y = Math.round(logo.y + logo.height) + LOGO_GAP;
		int step = BUTTON_HEIGHT + BUTTON_GAP;
		for (String key : KEPT_BUTTONS) {
			AbstractWidget button = byCaption(before, I18n.get(key));
			if (button != null) {
				button.x = x;
				button.y = y;
				button.setWidth(BUTTON_WIDTH);
				add.accept(button);
			}
			// A button the game left out keeps its row free rather than shifting the others.
			y += step;
		}
		add.accept(new ThemedButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.get("gui.tntsallin1client.menu.open_button"),
				() -> Minecraft.getInstance().setScreen(ClientMenus.create(screen))));

		ThemedButton quit = new ThemedButton(width - QUIT_SIZE - 6, 6, QUIT_SIZE, QUIT_SIZE, "", () -> Minecraft.getInstance().stop()) {
			@Override
			protected void renderLabel(int textColor) {
				ClientLogo.drawCross(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 8.0F, 1.5F, textColor);
			}
		};
		quit.tooltip = I18n.get("menu.quit");
		add.accept(quit);
		MenuButtons.addFriendsButton(screen, add, true);
	}

	private static AbstractWidget byCaption(List<AbstractWidget> buttons, String caption) {
		for (AbstractWidget button : buttons) {
			if (caption.equals(button.getMessage())) {
				return button;
			}
		}
		return null;
	}

	/** Theme background plus the version line - everything of the client layout that isn't a button. */
	public static void renderClientBackground(int width, int height) {
		ClientTheme theme = ClientTheme.get();
		GuiComponent.fill(0, 0, width, height, theme.background1);
		ClientFont.draw("Minecraft " + SharedConstants.getCurrentVersion().getName(), 2, height - 10, ClientTheme.withAlpha(theme.text, 0.5F));
	}

	/** Buttons fade in right after the switch animation ends, instead of popping in all at once. */
	public static float clientWidgetAlpha() {
		if (buttonFadeStart < 0) {
			return 1.0F;
		}
		float t = (Util.getMillis() - buttonFadeStart) / (float) BUTTON_FADE_MS;
		if (t >= 1.0F) {
			buttonFadeStart = -1;
			return 1.0F;
		}
		return Math.max(0.0F, t);
	}

	// --- Switching ----------------------------------------------------------------------------

	/** Client Design button in the game's own layout: animate over to the client design. */
	public static void switchToClient() {
		Minecraft client = Minecraft.getInstance();
		ClientDesign.setClient(true);
		transitionFrom = minecraftAnchor(client.window.getGuiScaledWidth(), client.window.getGuiScaledHeight());
		transitionToClient = true;
		transitionStart = Util.getMillis();
	}

	/**
	 * Logo button in the client layout: back to the game's own - whose buttons the caller builds right
	 * away, under the animation. `from` is where the logo sits in that screen's client layout.
	 */
	public static void switchToMinecraft(Rect from) {
		ClientDesign.setClient(false);
		transitionFrom = from;
		transitionToClient = false;
		transitionStart = Util.getMillis();
	}

	public static boolean isRunning() {
		return transitionStart >= 0;
	}

	/** Eased animation progress, 0..1. */
	public static float progress() {
		if (!isRunning()) {
			return 1.0F;
		}
		float t = Math.min(1.0F, (Util.getMillis() - transitionStart) / (float) TRANSITION_MS);
		return t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	/**
	 * Draws the animation on top of the game's own layout and reports whether it just finished - the
	 * caller then rebuilds the buttons (towards the client design) or simply stops. `clientLogo` is
	 * where the logo sits in that screen's client layout.
	 */
	public static boolean renderTransition(int width, int height, Rect clientLogo) {
		if (!isRunning()) {
			return false;
		}
		float t = progress();
		float backgroundAlpha = transitionToClient ? t : 1.0F - t;
		GuiComponent.fill(0, 0, width, height, ClientTheme.withAlpha(ClientTheme.get().background1, backgroundAlpha));
		Rect to = transitionToClient ? clientLogo : minecraftAnchor(width, height);
		Rect logo = transitionFrom.lerp(to, t);
		ClientLogo.draw(logo.x, logo.y, logo.height, 1.0F);

		if (Util.getMillis() - transitionStart < TRANSITION_MS) {
			return false;
		}
		if (transitionToClient) {
			buttonFadeStart = Util.getMillis();
		}
		transitionStart = -1;
		return true;
	}

	/** Leaving the screen mid-animation (shouldn't be possible, clicks are blocked) just ends it. */
	public static void cancel() {
		transitionStart = -1;
	}
}
